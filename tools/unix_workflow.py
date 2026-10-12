"""Unix ports of the three current PowerShell entrypoints; no Java code changes."""
import argparse
import hashlib
import http.server
import json
import os
from pathlib import Path
import re
import shutil
import socket
import ssl
import subprocess
import sys
import time
import threading
import urllib.request
import uuid
import zipfile

REPO = Path(__file__).resolve().parent.parent
SOURCE = REPO / "spike001a/compat-002-0l/src"
PROBE_SHA = "a774d2f737b17f089046014ab55a53767dbad008cd44dcce52c727b87e559ae8"
LIBRARIES = (
    "net/neoforged/fancymodloader/loader/4.0.45/loader-4.0.45.jar",
    "cpw/mods/securejarhandler/3.0.8/securejarhandler-3.0.8.jar",
    "cpw/mods/modlauncher/11.0.5/modlauncher-11.0.5.jar",
    "org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar",
    "org/apache/maven/maven-artifact/3.8.5/maven-artifact-3.8.5.jar",
    "org/ow2/asm/asm/9.10.1/asm-9.10.1.jar",
    "org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar",
    "net/neoforged/bus/8.0.5/bus-8.0.5.jar",
)
FLAGS = tuple("-Dforgecompat." + name + "=true" for name in (
    "2a.rewrite", "0g.apiRewrite", "0j.emptyRegistration", "0l.subscriberRewrite"))
MARKERS = (
    "FORGE PROBE CONSTRUCTOR REACHED",
    "SUBSCRIBER enabled=true annotationHits=1",
    "EVENT_PROFILE enabled=true superHits=1 constructorHits=1",
    "register returned target=com.example.forgeprobe.ForgeProbe declaredMethods=1",
    "delegate -> builtin javafml for neoforge",
    "NeoForge mod loading, version 21.1.256", "Done (",
)


class NotRunError(RuntimeError):
    """A required input or launch prerequisite is unavailable."""


def save(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n")


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def fingerprints(paths):
    return [{"Path": str(p), "SHA256": digest(p), "Bytes": p.stat().st_size}
            for p in sorted(paths)]


def unchanged(records):
    for record in records:
        path = Path(record["Path"])
        if not path.is_file() or digest(path) != record["SHA256"]:
            raise RuntimeError(f"Input changed: {path}")


def no_links(path):
    # Check lexical ancestors before resolving, so links cannot hide themselves.
    for candidate in (path, *path.parents):
        if candidate.is_symlink():
            raise RuntimeError(f"Symlink not allowed: {candidate}")
    if path.is_dir():
        for candidate in path.rglob("*"):
            if candidate.is_symlink():
                raise RuntimeError(f"Symlink not allowed: {candidate}")


def run_directory(root):
    no_links(root)
    root = root.resolve()
    if not root.is_relative_to(REPO / "out") or root == REPO / "out":
        raise RuntimeError("Output root must be strictly below checkout out/")
    directory = root / (time.strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:8])
    directory.mkdir(parents=True)
    return directory


def native(directory, name, command, cwd=REPO):
    save(directory / (name + ".command.json"), {
        "executable": str(command[0]), "arguments": list(map(str, command[1:])),
        "workingDirectory": str(cwd)})
    with (directory / (name + ".stdout.log")).open("wb") as out, \
            (directory / (name + ".stderr.log")).open("wb") as err:
        code = subprocess.run(list(map(str, command)), cwd=cwd, stdout=out, stderr=err).returncode
    save(directory / (name + ".exit.json"), {"exitCode": code})
    if code:
        raise RuntimeError(f"{name} exited {code}; see native logs")


def tools(args, directory):
    jdk = args.jdk_home.resolve()
    for name in ("java", "javac", "jar"):
        if not os.access(jdk / "bin" / name, os.X_OK):
            raise RuntimeError(f"Missing executable: {jdk / 'bin' / name}")
    native(directory, "java-version", [jdk / "bin/java", "-version"])
    version = (directory / "java-version.stderr.log").read_text() + (directory / "java-version.stdout.log").read_text()
    if 'version "21.0.10"' not in version or "Microsoft" not in version:
        raise RuntimeError("Requires Microsoft JDK 21.0.10+7-LTS")
    return jdk / "bin"


def probe(args):
    path = args.probe.resolve()
    if not path.is_file() or digest(path) != PROBE_SHA:
        raise NotRunError("Missing or incorrect fixed Probe; transfer original artifact, do not rebuild")
    return path


def jar_contents(path):
    with zipfile.ZipFile(path) as archive:
        return [{"Entry": name, "SHA256": hashlib.sha256(archive.read(name)).hexdigest(),
                 "Bytes": archive.getinfo(name).file_size}
                for name in sorted(archive.namelist()) if not name.endswith("/")]


def build(args, directory, binary):
    libraries = [args.server_input / "libraries" / name for name in LIBRARIES]
    for library in libraries:
        if not library.is_file():
            raise RuntimeError(f"Missing dependency: {library}")
    if list(SOURCE.rglob("*.class")):
        raise RuntimeError("Precompiled class in source tree")
    sources = sorted((SOURCE / "main/java").rglob("*.java"))
    if not sources:
        raise RuntimeError("No Java sources")
    inputs = fingerprints([p for p in SOURCE.rglob("*") if p.is_file()] + libraries
                          + [binary / name for name in ("java", "javac", "jar")])
    save(directory / "build-inputs.json", inputs)
    classes = directory / "classes"
    classes.mkdir()
    native(directory, "javac", [binary / "javac", "--release", "21", "-encoding", "UTF-8",
           "-classpath", os.pathsep.join(map(str, libraries)), "-d", classes, *sources])
    root = directory / "jar-root"
    shutil.copytree(classes, root)
    shutil.copytree(SOURCE / "main/resources", root, dirs_exist_ok=True)
    jar = directory / "forgecompat-002-0l.jar"
    native(directory, "jar", [binary / "jar", "--create", "--file", jar, "--manifest",
           SOURCE / "main/resources/META-INF/MANIFEST.MF", "-C", root, "."])
    unchanged(inputs)
    save(directory / "jar-contents.json", jar_contents(jar))
    result = {"status": "PASS", "javaCount": len(sources), "cleanOutput": True,
              "source": str(SOURCE), "jar": fingerprints([jar])[0]}
    save(directory / "build-result.json", result)
    return jar, result


def server_preflight(args, compat=None):
    if not args.allow_server_start:
        raise NotRunError("Server NOT RUN: requires explicit --allow-server-start authorization")
    if compat is not None:
        # Reject stale builds that cannot redirect dump I/O into this isolated run.
        with zipfile.ZipFile(compat) as archive:
            transformer = archive.read("com/example/forgecompat/ForgeProbeConstructorTransformer.class")
        if any(key not in transformer for key in (b"forgecompat.dump.path", b"forgecompat.dump.profileDir")):
            raise NotRunError("Server NOT RUN: rebuild with the approved configurable dump paths")
    processes = subprocess.run(["pgrep", "-x", "java"], capture_output=True)
    if processes.returncode not in (0, 1):
        raise NotRunError("Unable to establish Java process isolation")
    if processes.returncode == 0:
        raise NotRunError("Another Java process exists; NOT RUN")
    with socket.socket() as listener:
        try:
            listener.bind(("", 25565))
        except OSError as error:
            raise NotRunError("Cannot establish exclusive availability of port 25565") from error
    for folder in ("libraries", "config", "defaultconfigs", "spike000world"):
        path = args.server_input / folder
        if not path.is_dir():
            raise NotRunError(f"Missing isolated server input: {path}")
        no_links(path)
    argfile = args.server_input / "libraries/net/neoforged/neoforge/21.1.256/unix_args.txt"
    if not argfile.is_file():
        raise NotRunError(f"Missing official Unix named-module argfile: {argfile}")


def public_key_relay(directory):
    """Authlib uses Proxy.NO_PROXY; preserve HTTPS upstream via its supported host override."""
    context = ssl.create_default_context(cafile=os.environ.get("SSL_CERT_FILE"))
    events = []

    class Handler(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path != "/publickeys":
                self.send_error(404)
                return
            try:
                with urllib.request.urlopen("https://api.minecraftservices.com/publickeys",
                                            context=context, timeout=20) as response:
                    body = response.read()
                    status = response.status
                    content_type = response.headers.get("Content-Type", "application/json")
                events.append({"upstream": "https://api.minecraftservices.com/publickeys",
                               "status": status, "sha256": hashlib.sha256(body).hexdigest()})
                self.send_response(status)
                self.send_header("Content-Type", content_type)
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)
            except (OSError, ValueError) as error:
                events.append({"error": str(error)})
                self.send_error(502, "Verified upstream request failed")
            finally:
                save(directory / "public-key-relay.json", events)

        def log_message(self, *_):
            pass

    relay = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    thread = threading.Thread(target=relay.serve_forever, daemon=True)
    thread.start()
    return relay, thread


def harness(args, directory, binary, mod, compat, installed_mod_name="forgeprobe.jar"):
    # Never mutate the supplied installation. Every invocation gets a physical copy.
    records = fingerprints([p for folder in ("libraries", "config", "defaultconfigs", "spike000world")
                            for p in (args.server_input / folder).rglob("*") if p.is_file()]
                           + [mod] + ([compat] if compat else []))
    save(directory / "harness-inputs.json", records)
    server = directory / "server"
    server.mkdir()
    for folder in ("libraries", "config", "defaultconfigs", "spike000world"):
        shutil.copytree(args.server_input / folder, server / folder)
    mods = server / "mods"
    mods.mkdir()
    shutil.copyfile(mod, mods / installed_mod_name)
    if compat:
        shutil.copyfile(compat, mods / "forgecompat-002-0l.jar")
    # Preserve baseline properties except bind locally for the cloud's isolated check.
    original = (REPO / "spike000/run-server.ps1").read_text()
    block = original.split("$props = @(", 1)[1].split("\n)", 1)[0]
    properties = re.findall(r"^\s*'([^']*)',?\s*$", block, re.MULTILINE)
    properties = ["server-ip=127.0.0.1" if value == "server-ip=" else value for value in properties]
    (server / "server.properties").write_text("\n".join(properties) + "\n")
    (server / "eula.txt").write_text("eula=true\n")
    dump_args = (f"-Dforgecompat.dump.path={directory / 'dumps/rewritten/ForgeProbe.rewritten.class'}",
                 f"-Dforgecompat.dump.profileDir={directory / 'dumps/profile'}")
    (server / "user_jvm_args.txt").write_text("\n".join(json.dumps(value) for value in
            ("-Xmx3G", "-Xms1G", *FLAGS, *dump_args)) + "\n")
    network = ["@" + str(args.java_network_args.resolve())] if args.java_network_args else []
    relay, relay_thread = public_key_relay(directory) if args.public_key_relay else (None, None)
    endpoints = ([f"-Dminecraft.api.services.host=http://127.0.0.1:{relay.server_port}",
                  "-Dminecraft.api.session.host=https://sessionserver.mojang.com"] if relay else [])
    command = [binary / "java", *network, *endpoints, "@user_jvm_args.txt",
               "@libraries/net/neoforged/neoforge/21.1.256/unix_args.txt", "nogui"]
    logs = directory / "logs/spike002-0l-treatment"
    logs.mkdir(parents=True)
    label = "spike002-0l-treatment"
    save(logs / "command.json", {"arguments": list(map(str, command)), "workingDirectory": str(server)})
    started = time.time()
    state, done = "TIMEOUT", None
    with (logs / (label + ".stdout.log")).open("wb") as out, \
            (logs / (label + ".stderr.log")).open("wb") as err:
        process = subprocess.Popen(list(map(str, command)), cwd=server, stdin=subprocess.PIPE, stdout=out, stderr=err)
        try:
            while time.time() - started < args.max_wait_seconds:
                if process.poll() is not None:
                    state = "EXITED_EARLY"
                    break
                latest = server / "logs/latest.log"
                if latest.exists():
                    done = next((line for line in latest.read_text(errors="replace").splitlines() if "Done (" in line), None)
                    if done:
                        time.sleep(args.post_done_seconds)
                        process.stdin.write(b"stop\n")
                        process.stdin.flush()
                        try:
                            process.wait(timeout=120)
                            state = "DONE_STOPPED_GRACEFULLY"
                        except subprocess.TimeoutExpired:
                            state = "DONE_FORCED_KILL"
                        break
                time.sleep(1)
            if process.poll() is None:
                process.kill()
                process.wait(timeout=30)
                if state == "TIMEOUT":
                    state = "TIMEOUT_FORCED_KILL"
        finally:
            if process.poll() is None:
                process.kill()
                process.wait(timeout=30)
            process.stdin.close()
            if relay:
                relay.shutdown()
                relay.server_close()
                relay_thread.join(timeout=5)
    for filename, suffix in (("latest.log", ".log"), ("debug.log", ".debug.log")):
        source = server / "logs" / filename
        if source.exists():
            shutil.copyfile(source, logs / (label + suffix))
    if (server / "crash-reports").exists():
        shutil.copytree(server / "crash-reports", logs / "crash-reports")
    meta = {"status": state, "exitCode": process.returncode, "doneLine": done,
            "durationSeconds": round(time.time() - started, 1), "workingDirectory": str(server)}
    save(logs / "meta.json", meta)
    unchanged(records)
    return meta, logs, server


def assertions(directory, meta, logs, server, mod, compat, negative=None):
    label = "spike002-0l-treatment"
    stdout = (logs / (label + ".stdout.log")).read_text(errors="replace")
    debug = (logs / (label + ".debug.log")).read_text(errors="replace")
    checks = {"exactFixtureInstalled": digest(server / "mods/forgeprobe.jar") == digest(mod),
              "exactCompatInstalled": digest(server / "mods/forgecompat-002-0l.jar") == digest(compat)}
    if negative:
        target, actual = ("convergence_missing", "[MISSING]") if negative.startswith("D-") else ("minecraft", "1.21.1")
        checks.update(specificDependencyRejected=("Missing or unsupported mandatory dependencies" in debug
                      and f"Mod ID: '{target}', Requested by: 'forgeprobe'" in debug and f"Actual version: '{actual}'" in debug),
                      preLoadingFatal="Error during pre-loading phase" in stdout and "requires" in stdout and target in stdout,
                      noTargetInstantiation=not re.search(r"FORGE PROBE CONSTRUCTOR REACHED|register returned target=com.example.forgeprobe.ForgeProbe|loadMod called for forgeprobe", stdout),
                      noDone=meta["doneLine"] is None and "Done (" not in stdout,
                      exitedBeforeDone=meta["status"] == "EXITED_EARLY")
    else:
        checks.update({marker: marker in stdout for marker in MARKERS})
        checks["doneAndStopped"] = meta["status"] == "DONE_STOPPED_GRACEFULLY" and meta["exitCode"] == 0
        checks["no legacy empty no-op"] = "legacy-empty-noop" not in stdout
        texts = [stdout, debug, (logs / (label + ".log")).read_text(errors="replace")]
        checks["no ERROR/FATAL or dump FAIL"] = not any(re.search(r"\b(ERROR|FATAL)\b|dump FAIL", text) for text in texts)
        checks["stderr empty"] = (logs / (label + ".stderr.log")).stat().st_size == 0
        checks["no crash report"] = not any(p.is_file() for p in (logs / "crash-reports").rglob("*"))
        checks["exactly two mods"] = len(list((server / "mods").iterdir())) == 2
        discovery = f"Found additional transformation services from discovery services: [{server / 'mods/forgecompat-002-0l.jar'}]"
        checks["isolated artifact service discovery"] = (discovery in debug and "Loaded service forgecompat-transform" in debug
                                                        and "ForgeCompatLanguageLoader from forgecompat-002-0l.jar" in debug)
    save(directory / "assertions.json", checks)
    if not all(checks.values()):
        raise RuntimeError("Assertion failed; see assertions.json")
    return checks


def dependencies(args, directory, binary, fixed, compat, result):
    if compat.name != "forgecompat-002-0l.jar":
        raise RuntimeError("Keep the baseline compat jar filename")
    libraries = sorted((args.server_input / "libraries").rglob("*.jar"))
    if not libraries:
        raise NotRunError("Missing server libraries")
    fixtures = REPO / "tests/dependencies"
    inputs = fingerprints([compat, fixed, Path(__file__), *libraries, *fixtures.iterdir()])
    save(directory / "inputs.json", inputs)
    classes = directory / "classes"
    classes.mkdir()
    cp = os.pathsep.join(map(str, [compat, *libraries]))
    native(directory, "javac", [binary / "javac", "--release", "21", "-encoding", "UTF-8", "-classpath", cp,
           "-d", classes, fixtures / "DependencyContractTest.java"])
    native(directory, "boundary", [binary / "java", "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
           "--add-exports", "java.base/sun.security.util=ALL-UNNAMED", "-classpath", str(classes) + os.pathsep + cp,
           "com.example.forgecompat.DependencyContractTest", args.stage, fixtures, fixed])
    boundary = (directory / "boundary.stdout.log").read_text()
    marker = "PASS cases=6" if args.stage == "after" else "REPRODUCED cases=6"
    if marker not in boundary:
        raise RuntimeError("Boundary runner did not report six completed cases")
    result.update(boundary="PASS", cases=[])
    if not args.boundary_only:
        server_preflight(args, compat)
        names = ["D-missing-required"] if args.stage == "before" else ["C-satisfied", "D-missing-required", "E-missing-optional", "F-wrong-version"]
        for name in names:
            case = directory / name
            case.mkdir()
            root = case / "jar-root"
            with zipfile.ZipFile(fixed) as archive:
                for entry in archive.infolist():
                    if not (root / entry.filename).resolve().is_relative_to(root.resolve()):
                        raise RuntimeError("Unsafe ZIP entry")
                archive.extractall(root)
            shutil.copyfile(fixtures / (name + ".toml"), root / "META-INF/mods.toml")
            mod = case / "forgeprobe.jar"
            native(case, "jar", [binary / "jar", "--create", "--file", mod, "--manifest", root / "META-INF/MANIFEST.MF", "-C", root, "."])
            original_classes = [item for item in jar_contents(fixed) if item["Entry"].endswith(".class")]
            new_classes = [item for item in jar_contents(mod) if item["Entry"].endswith(".class")]
            if original_classes != new_classes:
                raise RuntimeError("Fixture changed probe bytecode")
            save(case / "fixture.json", {"classesUnchanged": True, "jar": fingerprints([mod])[0]})
            meta, logs, server = harness(args, case, binary, mod, compat)
            negative = name if args.stage == "after" and name[0] in "DF" else None
            checks = assertions(case, meta, logs, server, mod, compat, negative)
            result["cases"].append({"name": name, "status": "PASS", "negative": bool(negative), "meta": meta, "assertions": checks})
    unchanged(inputs)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("workflow", choices=("build", "dependencies", "harness"))
    parser.add_argument("--mode", choices=("build", "smoke", "regress"), default="build")
    parser.add_argument("--jdk-home", type=Path, default=Path(os.environ.get("JAVA_HOME", "/workspace/dualforge-cloud/tools/msjdk/usr/lib/jvm/msopenjdk-21-amd64")))
    parser.add_argument("--server-input", type=Path, default=REPO / "spike000/neoforge-server")
    parser.add_argument("--probe", type=Path, default=REPO / "spike000/probe-002-0l/build/forgeprobe.jar")
    parser.add_argument("--jar", "--compat-jar", dest="jar", type=Path)
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--stage", choices=("before", "after"), default="after")
    parser.add_argument("--boundary-only", action="store_true")
    parser.add_argument("--allow-server-start", action="store_true")
    parser.add_argument("--java-network-args", type=Path,
                        help="Optional platform proxy/CA JVM argfile; no TLS verification bypass")
    parser.add_argument("--public-key-relay", action="store_true",
                        help="Use Authlib host configuration with a loopback relay to verified official HTTPS")
    parser.add_argument("--max-wait-seconds", type=int, choices=range(1, 601), metavar="1..600", default=120)
    parser.add_argument("--post-done-seconds", type=int, choices=range(0, 601), metavar="0..600", default=10)
    args = parser.parse_args()
    args.server_input = args.server_input.absolute()
    directory = None
    stage = "BUILD" if args.workflow == "build" and args.mode != "smoke" else "NOT_RUN"
    result = {"status": "NOT RUN", "category": stage, "exitCode": 40, "workflow": args.workflow}
    try:
        directory = run_directory(args.output_root or REPO / "out/unix" / args.workflow)
        result["runDirectory"] = str(directory)
        save(directory / "invocation.json", {key: str(value) if isinstance(value, Path) else value for key, value in vars(args).items()})
        binary = tools(args, directory)
        compat = args.jar.resolve() if args.jar else None
        if args.workflow == "build" and args.mode != "smoke":
            compat, result["build"] = build(args, directory, binary)
        if args.workflow == "dependencies":
            if compat is None:
                raise RuntimeError("--compat-jar is required")
            fixed = probe(args)
            stage = "ASSERTION"
            dependencies(args, directory, binary, fixed, compat, result)
        elif args.workflow == "harness" or args.mode in ("smoke", "regress"):
            stage = "NOT_RUN"
            if compat is None or compat.name != "forgecompat-002-0l.jar":
                raise RuntimeError("--jar with the baseline filename is required")
            fixed = probe(args)
            server_preflight(args, compat)
            stage = "STARTUP"
            records = fingerprints([p for folder in ("spike000", "spike001a", "spike001b5", "Dev Manuals")
                                    for p in (REPO / folder).rglob("*") if p.is_file()] + [compat, fixed])
            save(directory / "preserved-before.json", records)
            try:
                meta, logs, server = harness(args, directory, binary, fixed, compat)
            finally:
                unchanged(records)
            if args.workflow != "harness":
                if meta["status"] != "DONE_STOPPED_GRACEFULLY" or meta["exitCode"] != 0:
                    raise RuntimeError("Startup failed; see meta.json")
                stage = "ASSERTION"
                result["assertions"] = assertions(directory, meta, logs, server, fixed, compat)
            elif meta["status"] != "DONE_STOPPED_GRACEFULLY" or meta["exitCode"] != 0:
                raise RuntimeError("Harness did not stop gracefully; see meta.json")
            result["meta"] = meta
        result.update(status="REPRODUCED" if args.workflow == "dependencies" and args.stage == "before" else "PASS", category="PASS", exitCode=0)
    except (OSError, RuntimeError, zipfile.BadZipFile) as error:
        if isinstance(error, NotRunError):
            stage = "NOT_RUN"
        code = {"BUILD": 10, "STARTUP": 20, "ASSERTION": 30}.get(stage, 40)
        if args.workflow == "dependencies" and code != 40:
            code = 1  # Preserve the independent PowerShell dependency-test failure code.
        result.update(status="PARTIAL" if code == 40 and ("build" in result or "boundary" in result) else "NOT RUN" if code == 40 else "FAIL",
                      category=stage, exitCode=code, error=str(error))
        if directory:
            (directory / "failure.txt").write_text(str(error) + "\n")
    finally:
        if directory:
            save(directory / "result.json", result)
        print(f"RESULT {result['status']} exit={result['exitCode']} {directory or ''}")
        if "error" in result:
            print(result["error"], file=sys.stderr)
    return result["exitCode"]


if __name__ == "__main__":
    sys.exit(main())
