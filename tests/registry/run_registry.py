"""Convergence-005: pinned native Forge input, serial shared harness, and bound evidence."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import traceback
import zipfile

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("registry_events", REPO / "tests/events/run_events.py")
events = importlib.util.module_from_spec(spec)
spec.loader.exec_module(events)
w = events.w
identity = events.identity
SOURCE = REPO / "tests/registry"
ROOT = REPO / "out/convergence-005"
ENTRY = "com.example.registryprobe.RegistryProbe"


def fingerprint(path):
    return w.fingerprints([path.resolve()])[0]


def preflight(args, run, binary=None):
    snapshot = ROOT / "preflight/inputs-before.json"
    if snapshot.exists():
        raise w.NotRunError("Initial snapshot already exists; preserve it and use a fresh worktree")
    tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=REPO).decode().rstrip("\0").split("\0")
    paths = [REPO / p for p in tracked] + [p for folder in ("libraries", "config", "defaultconfigs", "spike000world")
        for p in (args.server_input / folder).rglob("*") if p.is_file()] + [args.probe.resolve(), args.identity_fixture.resolve(),
        args.event_fixture.resolve(), args.event_driver.resolve(), args.event_rejected.resolve()]
    records = w.fingerprints(paths)
    w.save(snapshot, records)
    w.save(ROOT / "preflight/git.json", {"baseCommit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=REPO, text=True).strip(),
        "branch": subprocess.check_output(["git", "branch", "--show-current"], cwd=REPO, text=True).strip(),
        "status": subprocess.check_output(["git", "status", "--short"], cwd=REPO, text=True)})
    return {"status": "PASS", "inputs": len(records)}


def fixture(args, run, binary):
    forge = [args.forge_dependencies.resolve() / n for n, _ in identity.FORGE]
    for path, (_, sha) in zip(forge, identity.FORGE):
        assert w.digest(path) == sha, f"Pinned Forge dependency mismatch: {path}"
    libs = sorted((args.server_input / "libraries").rglob("*.jar"))
    minecraft = args.server_input / "libraries/net/minecraft/server/1.21.1-20240808.144430/server-1.21.1-20240808.144430-srg.jar"
    # This is the vanilla, remapped pre-patch jar, not neoforge-*-server.jar.
    with zipfile.ZipFile(minecraft) as archive:
        classes = [archive.read(n) for n in archive.namelist() if n.endswith(".class")]
        assert not any(b"net/neoforged/" in c or b"net/minecraftforge/" in c for c in classes), "Minecraft compile input contains mod-loader patches"
    vanilla = [p for p in libs if str(p.relative_to(args.server_input / "libraries")).startswith(
        ("com/google/", "com/mojang/", "it/unimi/", "org/slf4j/", "org/apache/logging/log4j/", "org/jetbrains/", "org/joml/"))]
    forge_cp = [*forge, minecraft, *vanilla]
    neo = args.server_input / "libraries/net/neoforged/neoforge/21.1.256/neoforge-21.1.256-server.jar"
    records = w.fingerprints([p for p in SOURCE.rglob("*") if p.is_file()] + [*forge, *libs])
    w.save(run / "inputs.json", records)
    w.save(run / "native-compile-inputs.json", w.fingerprints(forge_cp))
    shutil.copytree(SOURCE, run / "source")
    jars = {}
    for name, folder, cp in (("registry-probe", SOURCE / "src", forge_cp),
                             ("registry-driver", SOURCE / "driver", [neo, *libs])):
        classes = run / (name + "-classes")
        classes.mkdir()
        w.native(run, name + "-javac", [binary / "javac", "--release", "21", "-encoding", "UTF-8",
                 "-classpath", os.pathsep.join(map(str, cp)), "-d", classes, *sorted(folder.rglob("*.java"))], cwd=run)
        root = run / (name + "-root")
        shutil.copytree(classes, root)
        shutil.copytree(folder / "main/resources", root, dirs_exist_ok=True)
        jar = run / (name + ".jar")
        w.native(run, name + "-jar", [binary / "jar", "--create", "--file", jar,
                 "--manifest", root / "META-INF/MANIFEST.MF", "-C", root, "."], cwd=run)
        w.save(run / (name + "-contents.json"), w.jar_contents(jar))
        jars[name] = fingerprint(jar)
    with zipfile.ZipFile(run / "registry-probe.jar") as archive:
        entry = archive.read(ENTRY.replace(".", "/") + ".class")
        assert b"net/neoforged/" not in entry and b"com/example/forgecompat/" not in entry
        assert b"net/minecraftforge/registries/RegistryObject" in entry
    # Same entry bytes behind native metadata: scope check only, not a loadable Forge fixture.
    with zipfile.ZipFile(run / "registry-probe.jar") as source, zipfile.ZipFile(run / "native-control.jar", "w") as target:
        for name in source.namelist():
            target.writestr(name, source.read(name))
        target.writestr("META-INF/neoforge.mods.toml", source.read("META-INF/mods.toml").replace(b'[52,)', b'[4,)'))
    jars["native-control"] = fingerprint(run / "native-control.jar")
    for name, cls in (("probe", ENTRY), ("driver", "com.example.registrydriver.RegistryDriver"),
                      ("observer", "com.example.registrydriver.RegistryDriver$1")):
        jar = run / ("registry-probe.jar" if name == "probe" else "registry-driver.jar")
        w.native(run, "javap-" + name, [binary / "javap", "-p", "-v", "-c", "-classpath", jar, cls], cwd=run)
    driver = (run / "javap-driver.stdout.log").read_text()
    assert not re.search(r"(?:Method|InterfaceMethod) (?:net/minecraft/core/Registry.register|net/neoforged/neoforge/registries/(?:DeferredRegister|RegisterEvent)\.(?:create|register)|.*setAccessible|.*Unsafe)", driver)
    w.unchanged(records)
    return {"status": "PASS", "jars": jars, "nativeForgeCompilation": True,
            "assets": "NOT RUN: original user PNG unavailable"}


def baseline(args, run, binary):
    artifacts = [args.jar.resolve(), args.fixture.resolve(), args.driver.resolve()]
    w.server_preflight(args, artifacts[0])
    w.save(run / "artifacts.json", w.fingerprints(artifacts))
    meta, logs, server = w.harness(args, run, binary, artifacts[1], artifacts[0],
        installed_mod_name=artifacts[1].name, extra_mods=(artifacts[2],), extra_jvm_args=("-DDF005_REGISTER=true",))
    stdout = (logs / "spike002-0l-treatment.stdout.log").read_text(errors="replace")
    debug = (logs / "spike002-0l-treatment.debug.log").read_text(errors="replace")
    stderr = (logs / "spike002-0l-treatment.stderr.log").read_text(errors="replace")
    checks = {
        "exact installed artifacts": all(w.digest(server / "mods" / p.name) == w.digest(p) for p in artifacts),
        "discovered exactly once": debug.count('Found mod file "registry-probe.jar" [locator: forgecompat locator') == 1,
        "real Reader and metadata": "ForgeModReader: found META-INF/mods.toml" in stdout
            and "Found valid mod file registry-probe.jar with {registryprobe} mods" in debug,
        "direct first failure": "0l expected one same-file event profile for " + ENTRY in stdout + stderr,
        "failure before construction and Done": "[DF005] CONSTRUCTOR REACHED" not in stdout and "Done (" not in stdout,
        "JVM exited": meta["status"] == "EXITED_EARLY",
    }
    w.save(run / "assertions.json", checks)
    assert all(checks.values()), "Review actual first failure; do not presume registry success/failure"
    return {"status": "PASS", "observedBehavior": "REJECTED before GAME layer: one-event profile required",
            "meta": meta, "assertions": checks, "compat": fingerprint(args.jar)}


def contract(args, run, binary, runtime=None):
    libs = sorted((args.server_input / "libraries").rglob("*.jar"))
    cp = os.pathsep.join(map(str, [args.jar.resolve(), *libs]))
    records = w.fingerprints([args.jar.resolve(), args.fixture.resolve(), args.native_control.resolve(),
                              SOURCE / "RegistryBytecodeTest.java", *libs])
    w.save(run / "inputs.json", records)
    classes = run / "classes"
    classes.mkdir()
    w.native(run, "javac-contract", [binary / "javac", "--release", "21", "-classpath", cp,
             "-d", classes, SOURCE / "RegistryBytecodeTest.java"], cwd=run)
    w.native(run, "bytecode", [binary / "java", "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
             "--add-exports", "java.base/sun.security.util=ALL-UNNAMED", *w.FLAGS,
             f"-Dforgecompat.dump.profileDir={run / 'dumps'}", "-classpath", str(classes) + os.pathsep + cp,
             "com.example.forgecompat.RegistryBytecodeTest", args.fixture.resolve(), args.native_control.resolve(),
             runtime if runtime else "-", run / "bytes"], cwd=run)
    assert "PASS registry bytecode scope and atomicity" in (run / "bytecode.stdout.log").read_text()
    assert (run / "bytecode.stderr.log").stat().st_size == 0
    w.save(run / "bytes.json", w.fingerprints(sorted((run / "bytes").glob("*.class"))))
    w.unchanged(records)
    return {"status": "PASS", "compat": fingerprint(args.jar), "fixture": fingerprint(args.fixture)}


def dynamic(args, run, binary):
    enabled = args.stage == "treatment"
    value = str(enabled).lower()
    artifacts = [args.jar.resolve(), args.fixture.resolve(), args.driver.resolve()]
    records = w.fingerprints(artifacts)
    w.server_preflight(args, artifacts[0])
    w.save(run / "artifacts.json", records)
    meta, logs, server = w.harness(args, run, binary, artifacts[1], artifacts[0],
        installed_mod_name=artifacts[1].name, extra_mods=(artifacts[2],), extra_jvm_args=("-DDF005_REGISTER=" + value,))
    host_inputs = [record for record in json.loads((run / "harness-inputs.json").read_text())
                   if Path(record["Path"]).is_relative_to(args.server_input.resolve())]
    environment = {"hostInputs": host_inputs, "java": fingerprint(binary / "java"),
                   "actualJavaVersionSHA256": w.digest(run / "java-version.stderr.log"),
                   # Minecraft rewrites timestamp comments; compare every actual property.
                   "serverProperties": sorted(line for line in (server / "server.properties").read_text().splitlines()
                                              if line and not line.startswith("#")),
                   "commonJvmFlags": ["-Xmx3G", "-Xms1G", *w.FLAGS], "publicKeyRelay": args.public_key_relay,
                   "javaNetworkArgs": fingerprint(args.java_network_args) if args.java_network_args else None}
    w.save(run / "ct-environment.json", environment)
    label = "spike002-0l-treatment"
    stdout = (logs / (label + ".stdout.log")).read_text(errors="replace")
    debug = (logs / (label + ".debug.log")).read_text(errors="replace")
    injected = re.findall(r"\[DF005\] INJECTED MODBUS=(\d+)", stdout)
    actual = re.findall(r"\[DF005\] CONTAINER MODBUS=(\d+) GLOBALBUS=(\d+)", stdout)
    checks = {
        "exact three installed artifacts": all(w.digest(server / "mods" / p.name) == w.digest(p) for p in artifacts)
            and sorted(p.name for p in (server / "mods").iterdir()) == sorted(p.name for p in artifacts),
        "one discovery real metadata and entry": debug.count('Found mod file "registry-probe.jar" [locator: forgecompat locator') == 1
            and "Found valid mod file registry-probe.jar with {registryprobe} mods" in debug
            and "loadMod called for registryprobe entrypoints=[" + ENTRY + "]" in stdout,
        "only registry entry transformed": stdout.count("offered: com/example/registryprobe/RegistryProbe") == 1
            and "offered: com/example/registrydriver/" not in stdout and "profile=single-item" in stdout,
        "actual container MOD bus identity distinct from global": len(injected) == len(actual) == 1
            and injected[0] == actual[0][0] and actual[0][0] != actual[0][1],
        "one construction attachment follows switch": stdout.count("[DF005] CONSTRUCTOR REACHED") == 1
            and stdout.count("[DF005] ATTACH RETURNED") == int(enabled)
            and stdout.count("[DF005] ATTACH SKIPPED") == int(not enabled),
        "actual non-defaulting registry query": stdout.count("[DF005] QUERY phase=ServerStartedEvent id=registryprobe:ice_ether contains="
            + value + " optional=" + value + " suppliers=" + str(int(enabled))) == 1
            and stdout.count("[DF005] QUERY PASS") == 1,
        "supplier and holder evidence follows switch": stdout.count("[DF005] SUPPLIER REACHED") == int(enabled)
            and stdout.count("[DF005] VERIFIED Item key=registryprobe:ice_ether holder=net.neoforged.neoforge.registries.DeferredHolder sameObject=true air=false") == int(enabled),
        "real official supplier stack": (all(x in stdout for x in ("[DF005] FRAME net.neoforged.neoforge.registries.RegisterEvent.register",
            "[DF005] FRAME net.neoforged.neoforge.registries.DeferredRegister.addEntries",
            "[DF005] FRAME net.neoforged.fml.ModLoader.postEventWrapContainerInModOrder")) if enabled else "[DF005] FRAME" not in stdout),
        "builtin and native host delegated": "delegate -> builtin javafml for registrydriver" in stdout
            and "delegate -> builtin javafml for neoforge" in stdout,
        "Done graceful stop exit0": meta["status"] == "DONE_STOPPED_GRACEFULLY" and meta["exitCode"] == 0 and "Stopping server" in stdout,
        "no ERROR FATAL dump FAIL": not any(re.search(r"\b(ERROR|FATAL)\b|dump FAIL", text) for text in
            (stdout, debug, (logs / (label + ".log")).read_text(errors="replace"))),
        "empty stderr no crash": (logs / (label + ".stderr.log")).stat().st_size == 0
            and not any(p.is_file() for p in (logs / "crash-reports").rglob("*")),
    }
    w.save(run / "assertions.json", checks)
    assert all(checks.values()), "See actual C/T logs; registration return alone cannot pass"
    bytecode = run / "contract"
    bytecode.mkdir()
    verified = contract(args, bytecode, binary, run / "dumps/profile")
    w.unchanged(records)
    return {"status": "PASS", "compat": events.compat_record(records, artifacts[0]),
            "fixture": events.compat_record(records, artifacts[1]), "driver": events.compat_record(records, artifacts[2]),
            "environment": environment, "meta": meta, "assertions": checks, "runtimeBytecode": verified, "registered": enabled}


def assets(args, run, binary=None):
    records = fingerprint(args.fixture)
    names = ["textures/item/ice_ether.png", "models/item/ice_ether.json", "lang/en_us.json", "lang/zh_cn.json"]
    prefix = "assets/registryprobe/"
    checks = {}
    hashes = {}
    import hashlib
    with zipfile.ZipFile(args.fixture) as archive:
        for name in names:
            checks[name + " exists"] = prefix + name in archive.namelist()
            if checks[name + " exists"]:
                hashes[name] = hashlib.sha256(archive.read(prefix + name)).hexdigest()
        model = json.loads(archive.read(prefix + names[1]))
        checks["1.21.1 model and layer0"] = model == {"parent": "minecraft:item/generated", "textures": {"layer0": "registryprobe:item/ice_ether"}}
        for name, value in (("en_us", "DualForge Test Chip"), ("zh_cn", "DualForge 测试芯片")):
            checks[name + " exact localization"] = json.loads(archive.read(prefix + "lang/" + name + ".json")) == {"item.registryprobe.ice_ether": value}
        checks["manifest module identity"] = b"Automatic-Module-Name: registryprobe" in archive.read("META-INF/MANIFEST.MF")
        checks["TOML mod identity"] = b'modId="registryprobe"' in archive.read("META-INF/mods.toml")
        if checks[names[0] + " exists"]:
            import io
            from PIL import Image
            png = archive.read(prefix + names[0])
            with Image.open(io.BytesIO(png)) as image:
                image.load()
                checks["32x32 RGBA PNG with transparent and visible pixels"] = (image.format == "PNG" and image.size == (32, 32)
                    and image.mode == "RGBA" and image.getchannel("A").getextrema() == (0, 255))
    w.save(run / "assertions.json", checks)
    w.save(run / "resources.json", hashes)
    w.unchanged([records])
    if not checks[names[0] + " exists"]:
        return {"status": "NOT RUN", "reason": "Original user PNG unavailable; no substitute generated",
                "fixture": records, "resources": hashes, "assertions": checks}
    assert all(checks.values()), "Resource contract failed"
    return {"status": "PASS", "fixture": records, "resources": hashes, "assertions": checks}


def client(args, run, binary=None):
    environment = {"DISPLAY": os.environ.get("DISPLAY"), "WAYLAND_DISPLAY": os.environ.get("WAYLAND_DISPLAY"),
                   "dri": Path("/dev/dri").exists(), "clientInput": str(args.client_input) if args.client_input else None}
    w.save(run / "environment.json", environment)
    return {"status": "NOT RUN", "reason": "No installed graphical Minecraft client or display; manual real-client acceptance required",
            "environment": environment, "screenshots": [], "fixture": fingerprint(args.fixture)}


def regression(args, run, binary):
    assert args.allow_server_start, "Regression requires --allow-server-start"
    compat = fingerprint(args.jar)
    # Reuse the unchanged 004 runner for all earlier regressions. Its own input records
    # bind each actual JVM invocation to this compat; no second server harness exists.
    base = ["zsh", REPO / "tests/events/run-events.zsh"]
    common = ["--server-input", args.server_input.resolve(), "--jar", args.jar.resolve()]
    network = (["--java-network-args", args.java_network_args.resolve()] if args.java_network_args else []) + (["--public-key-relay"] if args.public_key_relay else [])
    if not (events.ROOT / "preflight/inputs-before.json").exists():
        w.native(run, "events-preflight", [*base, "--stage", "preflight", "--identity-fixture", args.identity_fixture.resolve(),
                 "--server-input", args.server_input.resolve(), "--probe", args.probe.resolve()], cwd=REPO)
    commands = {
        "events-contract": [*base, "--stage", "contract", *common, "--fixture", args.event_fixture.resolve(), "--rejected-fixture", args.event_rejected.resolve()],
        "events-control": [*base, "--stage", "control", *common, "--fixture", args.event_fixture.resolve(), "--driver", args.event_driver.resolve(),
                           "--rejected-fixture", args.event_rejected.resolve(), "--allow-server-start", *network],
        "events-treatment": [*base, "--stage", "treatment", *common, "--fixture", args.event_fixture.resolve(), "--driver", args.event_driver.resolve(),
                             "--rejected-fixture", args.event_rejected.resolve(), "--allow-server-start", *network],
        "prior-regressions": [*base, "--stage", "regression", *common, "--identity-fixture", args.identity_fixture.resolve(),
                              "--forge-dependencies", args.forge_dependencies.resolve(), "--probe", args.probe.resolve(), "--allow-server-start", *network],
    }
    results = {}
    for name, command in commands.items():
        w.native(run, name, command, cwd=REPO)
        match = re.search(r"RESULT PASS(?: exit=0)? (.+)", (run / (name + ".stdout.log")).read_text())
        assert match, "Missing real PASS from " + name
        result_path = Path(match.group(1)) / "result.json"
        result = json.loads(result_path.read_text())
        assert result["status"] == "PASS" and result["compat"]["SHA256"] == compat["SHA256"], "Regression cohort mismatch"
        w.unchanged([compat])
        results[name] = {"status": "PASS", "result": str(result_path), "compatSha256": result["compat"]["SHA256"]}
        w.save(run / "regressions.json", results)
    return {"status": "PASS", "compat": compat, "regressions": results}


def cohort_consistent(results):
    stages = ("contract", "control", "treatment", "regression")
    hashes = {stage: results.get(stage, {}).get("compat", {}).get("SHA256") for stage in stages}
    if not all(isinstance(h, str) and re.fullmatch(r"[a-f0-9]{64}", h) for h in hashes.values()) or len(set(hashes.values())) != 1:
        return False
    # Verify actual stage roles and the nested runtime checker, not positional fingerprint order.
    for stage in stages:
        if Path(results[stage]["compat"].get("Path", "")).name != "forgecompat-002-0l.jar":
            return False
    c, t = results["control"], results["treatment"]
    if any(c.get(k) != t.get(k) for k in ("compat", "fixture", "driver")):
        return False
    if not c.get("environment") or c["environment"] != t.get("environment"):
        return False
    if results["contract"].get("fixture") != t.get("fixture"):
        return False
    fixture_record = results.get("fixture", {}).get("jars", {}).get("registry-probe")
    if fixture_record != t.get("fixture"):
        return False
    if results.get("fixture", {}).get("jars", {}).get("registry-driver") != t.get("driver"):
        return False
    for dynamic_result in (c, t):
        nested = dynamic_result.get("runtimeBytecode", {})
        if nested.get("compat") != dynamic_result.get("compat") or nested.get("fixture") != dynamic_result.get("fixture"):
            return False
    for stage in ("assets", "client"):
        if stage in results and results[stage].get("fixture") != t.get("fixture"):
            return False
    return True


def publish_result(stage, run, result):
    path = ROOT / "stage-results.json"
    index = json.loads(path.read_text()) if path.exists() else {}
    index[stage] = str(run / "result.json")
    w.save(path, index)
    results = {name: json.loads(Path(p).read_text()) for name, p in index.items()}
    required = {"fixture", "baseline", "contract", "control", "treatment", "regression"}
    complete = required.issubset(results)
    consistent = cohort_consistent(results) if complete else False
    server = "PARTIAL"
    if any(results.get(n, {}).get("status") == "FAIL" for n in required): server = "FAIL"
    elif complete and all(results[n]["status"] == "PASS" for n in required): server = "PASS" if consistent else "FAIL"
    assets_pass = results.get("assets", {}).get("status") == "PASS"
    w.save(ROOT / "summary.json", {"task": "Convergence-005", "serverRegistryChecks": server,
        "005-S": "FAIL" if results.get("assets", {}).get("status") == "FAIL" else server if assets_pass or server == "FAIL" else "PARTIAL",
        "005-C": results.get("client", {}).get("status", "NOT RUN"),
        "sameArtifactCohort": consistent, "evidence": index,
        "stageCompatSha256": {s: results.get(s, {}).get("compat", {}).get("SHA256") for s in ("contract", "control", "treatment", "regression")},
        "assets": results.get("assets", {}).get("status", "NOT RUN")})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stage", choices=("preflight", "fixture", "baseline", "contract", "control", "treatment", "assets", "client", "regression"), required=True)
    for name in ("forge-dependencies", "fixture", "driver", "jar", "native-control", "java-network-args", "identity-fixture",
                 "event-fixture", "event-driver", "event-rejected", "client-input"):
        parser.add_argument("--" + name, type=Path)
    parser.add_argument("--probe", type=Path, default=Path("/workspace/dualforge-cloud/inputs/forgeprobe.jar"))
    parser.add_argument("--server-input", type=Path, default=Path("/workspace/dualforge-cloud/server-base"))
    parser.add_argument("--jdk-home", type=Path, default=Path(os.environ.get("JAVA_HOME", "/workspace/dualforge-cloud/tools/msjdk/usr/lib/jvm/msopenjdk-21-amd64")))
    parser.add_argument("--allow-server-start", action="store_true")
    parser.add_argument("--public-key-relay", action="store_true")
    parser.add_argument("--max-wait-seconds", type=int, default=120)
    parser.add_argument("--post-done-seconds", type=int, default=10)
    args = parser.parse_args()
    run = w.run_directory(ROOT / args.stage)
    w.save(run / "invocation.json", {k: str(v) if isinstance(v, Path) else v for k, v in vars(args).items()})
    result = {"status": "FAIL", "stage": args.stage, "directory": str(run)}
    try:
        required = {"preflight": ("identity_fixture", "event_fixture", "event_driver", "event_rejected"),
                    "fixture": ("forge_dependencies",), "baseline": ("jar", "fixture", "driver"),
                    "contract": ("jar", "fixture", "native_control"),
                    "control": ("jar", "fixture", "driver", "native_control"),
                    "treatment": ("jar", "fixture", "driver", "native_control"), "assets": ("fixture",), "client": ("fixture",),
                    "regression": ("jar", "identity_fixture", "forge_dependencies", "event_fixture", "event_driver", "event_rejected")}[args.stage]
        for name in required:
            if getattr(args, name) is None or not getattr(args, name).exists():
                raise w.NotRunError("Missing explicit input: " + name)
        binary = None if args.stage in ("preflight", "assets", "client") else w.tools(args, run)
        result.update(dynamic(args, run, binary) if args.stage in ("control", "treatment") else globals()[args.stage](args, run, binary))
    except Exception as error:
        result.update(status="NOT RUN" if isinstance(error, w.NotRunError) else "FAIL", error=str(error))
        (run / "failure.txt").write_text(traceback.format_exc())
        raise
    finally:
        w.save(run / "result.json", result)
        publish_result(args.stage, run, result)
        print("RESULT", result["status"], run)


if __name__ == "__main__":
    main()
