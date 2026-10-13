"""Convergence-004: serial native fixtures, controlled bytecode, real FML C/T and regressions."""
import argparse
import hashlib
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
spec = importlib.util.spec_from_file_location("identity", REPO / "tests/identity/run_identity.py")
identity = importlib.util.module_from_spec(spec)
spec.loader.exec_module(identity)
w = identity.w
SOURCE = REPO / "tests/events"
ROOT = REPO / "out/convergence-004"
ENTRY = "com.example.dispatchcheck.DispatchProbe"
CALLBACK = "[DF004] CALLBACK REACHED"


def preflight(args, run):
    """Record the initial checkout and immutable external inputs before any build or edit."""
    snapshot = ROOT / "preflight/inputs-before.json"
    if snapshot.exists():
        raise w.NotRunError("Initial snapshot already exists; preserve it and use a fresh worktree for a new baseline")
    assert args.server_input.is_dir()
    assert w.digest(args.probe.resolve()) == w.PROBE_SHA
    assert w.digest(args.identity_fixture.resolve()) == "95ad77b0c2f5005bd7e98af139a911559635d4dd55e25463052a463748161b22"
    tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=REPO).decode().rstrip("\0").split("\0")
    paths = [REPO / name for name in tracked] + [p for folder in ("libraries", "config", "defaultconfigs", "spike000world")
        for p in (args.server_input / folder).rglob("*") if p.is_file()] + [args.probe.resolve(), args.identity_fixture.resolve()]
    records = w.fingerprints(paths)
    w.save(run / "inputs-before.json", records)
    w.save(snapshot, records)
    state = {"baseCommit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=REPO, text=True).strip(),
        "branch": subprocess.check_output(["git", "branch", "--show-current"], cwd=REPO, text=True).strip(),
        "status": subprocess.check_output(["git", "status", "--short"], cwd=REPO, text=True)}
    w.save(ROOT / "preflight/git.json", state)
    w.save(run / "git.json", state)
    return {"status": "PASS", "inputCount": len(records)}


def inputs(args, extra=()):
    return w.fingerprints([p for p in SOURCE.rglob("*") if p.is_file()] + list(extra)
                          + [REPO / "tools/lib/unix_workflow.py"])


def compat_record(records, path):
    matches = [record for record in records if record["Path"] == str(path.resolve())]
    if len(matches) != 1:
        raise RuntimeError(f"Expected exactly one actual compat input fingerprint: {path}")
    return matches[0]


def fixture(args, run, binary):
    forge = [args.forge_dependencies.resolve() / n for n, _ in identity.FORGE]
    for p, (_, h) in zip(forge, identity.FORGE):
        assert p.is_file() and w.digest(p) == h, f"Pinned Forge dependency missing/mismatch: {p}"
    libs = sorted((args.server_input / "libraries").rglob("*.jar"))
    assert libs, "Missing pinned host dependencies"
    records = inputs(args, [*forge, *libs])
    w.save(run / "inputs.json", records)
    shutil.copytree(SOURCE, run / "source")
    jars = {}
    for name, folder, cp in (("dispatch-probe", SOURCE / "src", forge),
                             ("dispatch-driver", SOURCE / "driver", libs),
                             ("rejected-probe", SOURCE / "controls", forge)):
        classes = run / (name + "-classes")
        classes.mkdir()
        java = sorted(folder.rglob("*.java"))
        w.native(run, name + "-javac", [binary / "javac", "--release", "21", "-encoding", "UTF-8",
                 "-classpath", os.pathsep.join(map(str, cp)), "-d", classes, *java], cwd=run)
        root = run / (name + "-root")
        shutil.copytree(classes, root)
        resources = SOURCE / "src/main/resources" if name == "rejected-probe" else folder / "main/resources"
        shutil.copytree(resources, root, dirs_exist_ok=True)
        jar = run / (name + ".jar")
        w.native(run, name + "-jar", [binary / "jar", "--create", "--file", jar,
                 "--manifest", root / "META-INF/MANIFEST.MF", "-C", root, "."], cwd=run)
        w.save(run / (name + "-contents.json"), w.jar_contents(jar))
        jars[name] = w.fingerprints([jar])[0]
    checks = {}
    with zipfile.ZipFile(run / "dispatch-probe.jar") as archive:
        entry = archive.read(ENTRY.replace(".", "/") + ".class")
        event = archive.read("com/example/dispatchcheck/DispatchEvent.class")
        checks["Forge-only fixture"] = b"net/neoforged" not in entry + event
        checks["Forge entry ABI and members"] = all(x in entry for x in
            (b"net/minecraftforge/fml/common/Mod", b"net/minecraftforge/eventbus/api/SubscribeEvent",
             b"net/minecraftforge/eventbus/api/IEventBus", b"net/minecraftforge/common/MinecraftForge"))
        checks["Forge Event superclass"] = b"net/minecraftforge/eventbus/api/Event" in event
    for name, jar, cls in (("entry", "dispatch-probe.jar", ENTRY),
                           ("event", "dispatch-probe.jar", "com.example.dispatchcheck.DispatchEvent"),
                           ("driver", "dispatch-driver.jar", "com.example.dispatchdriver.DispatchDriver"),
                           ("observer", "dispatch-driver.jar", "com.example.dispatchdriver.DispatchDriver$1")):
        w.native(run, "javap-" + name, [binary / "javap", "-p", "-v", "-c", "-classpath", run / jar, cls], cwd=run)
    for name, module in (("dispatch-probe", "dispatchprobe"), ("dispatch-driver", "dispatchdriver")):
        with zipfile.ZipFile(run / (name + ".jar")) as archive:
            checks[name + " explicit module identity"] = ("Automatic-Module-Name: " + module).encode() in archive.read("META-INF/MANIFEST.MF")
    driver_text = (run / "javap-driver.stdout.log").read_text()
    checks["driver contains one actual native post call"] = len(re.findall(r"InterfaceMethod net/neoforged/bus/api/IEventBus.post:", driver_text)) == 1
    checks["no direct listener call or private access"] = not re.search(r"(?:Method|InterfaceMethod) com/example/dispatchcheck/DispatchProbe\.|setAccessible|Unsafe|IEventBus.start:", driver_text)
    with zipfile.ZipFile(run / "dispatch-driver.jar") as archive:
        checks["callback text only in Forge fixture"] = not any(CALLBACK.encode() in archive.read(n) for n in archive.namelist())
    w.save(run / "assertions.json", checks)
    assert all(checks.values()), "Native fixture purity checks failed"
    w.unchanged(records)
    return {"status": "PASS", "jars": jars, "assertions": checks}


def contract(args, run, binary, runtime=None):
    libs = sorted((args.server_input / "libraries").rglob("*.jar"))
    cp = os.pathsep.join(map(str, [args.jar.resolve(), *libs]))
    records = inputs(args, [args.jar.resolve(), args.fixture.resolve(), args.rejected_fixture.resolve(), *libs])
    w.save(run / "inputs.json", records)
    classes = run / "test-classes"
    classes.mkdir()
    w.native(run, "javac-bytecode", [binary / "javac", "--release", "21", "-encoding", "UTF-8",
             "-classpath", cp, "-d", classes, SOURCE / "EventBytecodeTest.java"], cwd=run)
    out = run / "bytes"
    w.native(run, "bytecode", [binary / "java", "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
             "--add-exports", "java.base/sun.security.util=ALL-UNNAMED", *w.FLAGS,
             f"-Ddualforge.dump.path={run / 'dumps/entry.class'}",
             f"-Ddualforge.dump.profileDir={run / 'dumps/profile'}", "-classpath", str(classes) + os.pathsep + cp,
             "io.github.gfander.dualforge.EventBytecodeTest", args.fixture.resolve(), args.rejected_fixture.resolve(),
             str(runtime) if runtime else "-", out], cwd=run)
    checks = {"complete controlled bytecode and atomicity": "PASS controlled dispatch bytecode and atomicity" in (run / "bytecode.stdout.log").read_text(),
              "bytecode stderr empty": (run / "bytecode.stderr.log").stat().st_size == 0}
    for path in sorted(out.glob("*.class")):
        w.native(run, "javap-" + path.stem, [binary / "javap", "-p", "-v", "-c", path], cwd=run)
    w.save(run / "bytes.json", w.fingerprints(list(out.glob("*.class"))))
    w.save(run / "assertions.json", checks)
    assert all(checks.values())
    w.unchanged(records)
    return {"status": "PASS", "assertions": checks, "compat": compat_record(records, args.jar)}


def dynamic(args, run, binary):
    enabled = args.stage == "treatment"
    value = str(enabled).lower()
    artifacts = [args.jar.resolve(), args.fixture.resolve(), args.driver.resolve()]
    assert [p.name for p in artifacts] == ["dualforge.jar", "dispatch-probe.jar", "dispatch-driver.jar"]
    w.server_preflight(args, artifacts[0])
    records = inputs(args, artifacts)
    w.save(run / "inputs.json", {"files": records, "registrationSwitch": {
        "mechanism": "JVM system property", "argument": "-DDF004_REGISTER=" + value,
        "value": value, "readAt": ENTRY + ".<init>(Forge IEventBus)"}})
    meta, logs, server = w.harness(args, run, binary, artifacts[1], artifacts[0],
        installed_mod_name=artifacts[1].name, extra_mods=(artifacts[2],), extra_jvm_args=("-DDF004_REGISTER=" + value,))
    label = "spike002-0l-treatment"
    stdout = (logs / (label + ".stdout.log")).read_text(errors="replace")
    debug = (logs / (label + ".debug.log")).read_text(errors="replace")
    callback_count = stdout.count(CALLBACK)
    tokens = ["[DF004] CONSTRUCTOR REACHED", "[DF004] REGISTER RETURNED" if enabled else "[DF004] REGISTER SKIPPED",
              "[DF004] POST BEGIN"] + ([CALLBACK] if enabled else []) + ["[DF004] POST RETURNED"]
    checks = {"same exact installed artifacts": all(w.digest(server / "mods" / p.name) == w.digest(p) for p in artifacts),
        "exact three mods": sorted(p.name for p in (server / "mods").iterdir()) == sorted(p.name for p in artifacts),
        "identified once": debug.count('Found mod file "dispatch-probe.jar" [locator: dualforge locator') == 1,
        "real metadata and entry": "Found valid mod file dispatch-probe.jar with {dispatchprobe} mods" in debug
            and f"loadMod called for dispatchprobe entrypoints=[{ENTRY}]" in stdout,
        "actual switch arrived": stdout.count("[DF004] REGISTER SWITCH=" + value) == 1,
        "markers once and ordered": all(stdout.count(t) == 1 for t in tokens) and [stdout.find(t) for t in tokens] == sorted(stdout.find(t) for t in tokens),
        "one post": stdout.count("[DF004] POST BEGIN") == stdout.count("[DF004] POST RETURNED") == 1,
        "callback expected count": callback_count == int(enabled),
        "expected registration path": ("delegate to NeoForge register" in stdout) == enabled
            and ("[DF004] REGISTER SKIPPED" in stdout) != enabled,
        "native driver and host delegated": "delegate -> builtin javafml for dispatchdriver" in stdout
            and "delegate -> builtin javafml for neoforge" in stdout and "NeoForge mod loading, version 21.1.256" in stdout,
        "after-start driver event/module proven": "[DF004] DRIVER READY phase=ServerStartedEvent module=" in stdout
            and "event=com.example.dispatchcheck.DispatchEvent" in stdout,
        "Done stop exit0": meta["status"] == "DONE_STOPPED_GRACEFULLY" and meta["exitCode"] == 0 and "Stopping server" in stdout,
        "no ERROR FATAL dump FAIL": not any(re.search(r"\b(ERROR|FATAL)\b|dump FAIL", t) for t in [stdout, debug, (logs / (label + ".log")).read_text(errors="replace")]),
        "stderr empty": (logs / (label + ".stderr.log")).stat().st_size == 0,
        "no crash": not any(p.is_file() for p in (logs / "crash-reports").rglob("*")),
        "exact entry and event transformation": stdout.count("offered: com/example/dispatchcheck/DispatchProbe") == 1 and stdout.count("offered: com/example/dispatchcheck/DispatchEvent") == 1}
    if enabled:
        run_ids = re.findall(r"\[DF004\] DRIVER RUN=([\w-]+)", stdout)
        checks["stack at actual listener print matches run"] = len(run_ids) == 1 and stdout.count("[DF004] CALLBACK STACK RUN=" + run_ids[0] + " TOP=" + ENTRY + ".onDispatch") == 1
        checks["real bus chain captured"] = "[DF004] FRAME net.neoforged.bus.EventBus.post" in stdout and "[DF004] FRAME com.example.dispatchdriver.DispatchDriver.onStarted" in stdout
        checks["actual register returned target"] = f"register returned target={ENTRY} declaredMethods=1" in stdout
    else:
        checks["no callback stack or registration return"] = "[DF004] CALLBACK STACK" not in stdout and "register returned target=" + ENTRY not in stdout
    w.save(run / "assertions.json", checks)
    w.save(run / "meta.json", meta)
    w.save(run / "artifacts.json", w.fingerprints(artifacts))
    assert all(checks.values()), "See raw logs and assertions.json; stop this path"
    # Reuse the same bytecode checker against the actual runtime dumps after the server has exited.
    verify = run / "bytecode"
    verify.mkdir()
    contract_result = contract(args, verify, binary, run / "dumps/profile")
    w.unchanged(records)
    return {"status": "PASS", "registrationExecuted": stdout.count("[DF004] REGISTER RETURNED") == 1, "postCount": stdout.count("[DF004] POST BEGIN"),
            "callbackCount": callback_count, "meta": meta, "assertions": checks, "runtimeBytecode": contract_result,
            "compat": compat_record(records, args.jar)}


def regression(args, run, binary):
    compat = w.fingerprints([args.jar.resolve()])[0]
    w.save(run / "inputs.json", {"compat": compat})
    network = (["--java-network-args", args.java_network_args.resolve()] if args.java_network_args else []) + (["--public-key-relay"] if args.public_key_relay else [])
    common = ["--server-input", args.server_input.resolve(), "--jar", args.jar.resolve()]
    commands = {
        "original-smoke": ["zsh", REPO / "tools/entry/dualforge.zsh", "--mode", "smoke", *common,
                           "--probe", args.probe.resolve(), "--output-root", ROOT / "regression/original-smoke", "--allow-server-start", *network],
        "identity": ["zsh", REPO / "tests/identity/run-identity.zsh", "--stage", "after", *common,
                     "--fixture", args.identity_fixture.resolve(), "--allow-server-start", *network],
        "scope": ["zsh", REPO / "tests/identity/run-scope.zsh", *common, "--fixture", args.identity_fixture.resolve(),
                  "--forge-dependencies", args.forge_dependencies.resolve()],
        "discovery": ["zsh", REPO / "tests/discovery/run-discovery.zsh", args.jar.resolve(), args.server_input.resolve(), args.probe.resolve()],
        "dependencies": ["zsh", REPO / "tools/entry/test-forge-dependencies.zsh", "--stage", "after", "--compat-jar", args.jar.resolve(),
                         "--server-input", args.server_input.resolve(), "--probe", args.probe.resolve(), "--output-root", ROOT / "regression/dependencies",
                         "--allow-server-start", *network]}
    results = {}
    assert args.allow_server_start, "Regression requires --allow-server-start"
    assert w.digest(args.identity_fixture.resolve()) == "95ad77b0c2f5005bd7e98af139a911559635d4dd55e25463052a463748161b22", "Requires the frozen 003b IdentityProbe"
    for name, command in commands.items():
        w.native(run, name, command, cwd=REPO)
        match = re.search(r"RESULT PASS(?: exit=0)? (.+)", (run / (name + ".stdout.log")).read_text())
        assert match, f"Missing real PASS from {name}"
        path = Path(match.group(1)) / "result.json"
        result = json.loads(path.read_text())
        assert result["status"] == "PASS"
        input_file = path.parent / ("harness-inputs.json" if name == "original-smoke" else "inputs.json")
        actual = compat_record(json.loads(input_file.read_text()), args.jar)
        if actual["SHA256"] != compat["SHA256"]:
            raise RuntimeError(f"Regression {name} used a different compat SHA-256")
        w.unchanged([compat])
        results[name] = {"status": result["status"], "result": str(path), "compatSha256": actual["SHA256"]}
        w.save(run / "regressions.json", results)
    before = json.loads((ROOT / "preflight/inputs-before.json").read_text())
    allowed = {str(REPO / p) for p in ["dualforge/src/main/java/io/github/gfander/dualforge/DualForgeEntryTransformer.java",
        "tools/lib/unix_workflow.py", "tests/identity/IdentityScopeTest.java", "README.md", "tests/README.md"]}
    preserved = [r for r in before if r["Path"] not in allowed]
    w.unchanged(preserved)
    w.save(ROOT / "regression/preservation.json", {"status": "PASS", "unchangedFiles": preserved, "allowedChanges": sorted(allowed),
        "historyAllTrackedFilesUnchanged": True, "structureMappingUnchanged": True, "probeAndFrozenIdentityUnchanged": True})
    return {"status": "PASS", "regressions": results, "compat": compat,
            "preservation": "out/convergence-004/regression/preservation.json"}


def publish_result(stage, run, result):
    """Aggregate observed results; absent stages stay PARTIAL, failed stages never become PASS."""
    index_path = ROOT / "stage-results.json"
    index = json.loads(index_path.read_text()) if index_path.exists() else {}
    index[stage] = str(run / "result.json")
    w.save(index_path, index)
    results = {name: json.loads(Path(path).read_text()) for name, path in index.items()}
    required = {"fixture", "contract", "control", "treatment", "regression"}
    statuses = [r["status"] for r in results.values()]
    status = "FAIL" if "FAIL" in statuses else "NOT RUN" if "NOT RUN" in statuses else "PARTIAL"
    summary = {"task": "Convergence-004", "status": status,
        "baseCommit": json.loads((ROOT / "preflight/git.json").read_text())["baseCommit"] if (ROOT / "preflight/git.json").exists() else None,
        "testedCommit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=REPO, text=True).strip(),
        "evidence": index, "regressions": results.get("regression", {}).get("regressions", {})}
    stage_hashes = {name: results.get(name, {}).get("compat", {}).get("SHA256")
                    for name in ("contract", "control", "treatment", "regression")}
    summary["stageCompatSha256"] = stage_hashes
    summary["sameCompatAcrossStages"] = (all(isinstance(value, str) and re.fullmatch(r"[0-9a-f]{64}", value)
                                             for value in stage_hashes.values())
                                         and len(set(stage_hashes.values())) == 1)
    for name in ("control", "treatment"):
        if name in results:
            summary[name] = {key: results[name].get(key) for key in ("status", "registrationExecuted", "postCount", "callbackCount")}
    if required.issubset(index) and summary["baseCommit"] and all(r["status"] == "PASS" for r in results.values()):
        control = json.loads((Path(index["control"]).parent / "artifacts.json").read_text())
        treatment = json.loads((Path(index["treatment"]).parent / "artifacts.json").read_text())
        summary["identicalArtifactPathsAndSha256"] = control == treatment
        hashes = {Path(r["Path"]).name: r["SHA256"] for r in treatment}
        summary.update(compatSha256=hashes["dualforge.jar"],
                       forgeFixtureSha256=hashes["dispatch-probe.jar"], driverSha256=hashes["dispatch-driver.jar"])
        summary["ctCompatEvidenceConsistent"] = (summary["compatSha256"] == stage_hashes["control"]
                                                 == stage_hashes["treatment"])
        summary["status"] = ("PASS" if control == treatment and summary["sameCompatAcrossStages"]
                             and summary["ctCompatEvidenceConsistent"] else "FAIL")
    w.save(ROOT / "summary.json", summary)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--stage", choices=("preflight", "fixture", "contract", "control", "treatment", "regression"), required=True)
    for name in ("forge-dependencies", "fixture", "driver", "rejected-fixture", "jar", "identity-fixture", "java-network-args"):
        p.add_argument("--" + name, type=Path)
    p.add_argument("--probe", type=Path, default=Path("/workspace/dualforge-cloud/inputs/forgeprobe.jar"))
    p.add_argument("--server-input", type=Path, default=Path("/workspace/dualforge-cloud/server-base"))
    p.add_argument("--jdk-home", type=Path, default=Path(os.environ.get("JAVA_HOME", "/workspace/dualforge-cloud/tools/msjdk/usr/lib/jvm/msopenjdk-21-amd64")))
    p.add_argument("--allow-server-start", action="store_true")
    p.add_argument("--public-key-relay", action="store_true")
    p.add_argument("--max-wait-seconds", type=int, default=120)
    p.add_argument("--post-done-seconds", type=int, default=10)
    args = p.parse_args()
    run = w.run_directory(ROOT / args.stage)
    w.save(run / "invocation.json", {k: str(v) if isinstance(v, Path) else v for k, v in vars(args).items()})
    result = {"status": "FAIL", "stage": args.stage, "directory": str(run)}
    try:
        required = {"preflight": ("identity_fixture",), "fixture": ("forge_dependencies",), "contract": ("jar", "fixture", "rejected_fixture"),
                    "control": ("jar", "fixture", "driver", "rejected_fixture"),
                    "treatment": ("jar", "fixture", "driver", "rejected_fixture"),
                    "regression": ("jar", "forge_dependencies", "identity_fixture")}[args.stage]
        for name in required:
            path = getattr(args, name)
            if path is None or not path.exists(): raise w.NotRunError("Missing explicit input: " + name)
        binary = None if args.stage == "preflight" else w.tools(args, run)
        if args.stage == "preflight": result.update(preflight(args, run))
        elif args.stage == "fixture": result.update(fixture(args, run, binary))
        elif args.stage == "contract": result.update(contract(args, run, binary))
        elif args.stage == "regression": result.update(regression(args, run, binary))
        else: result.update(dynamic(args, run, binary))
    except Exception as error:
        result.update(status="NOT RUN" if isinstance(error, w.NotRunError) else "FAIL", error=str(error))
        (run / "failure.txt").write_text(traceback.format_exc())
        raise
    finally:
        w.save(run / "result.json", result)
        publish_result(args.stage, run, result)
        print("RESULT", result["status"], run)


if __name__ == "__main__": main()
