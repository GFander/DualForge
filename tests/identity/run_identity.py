"""003b Forge fixture and isolated dynamic checks, reusing the Linux harness."""
import argparse
import importlib.util
import os
from pathlib import Path
import re
import shutil
import zipfile

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("workflow", REPO / "tools/lib/unix_workflow.py")
w = importlib.util.module_from_spec(spec)
spec.loader.exec_module(w)
FORGE = (
    ("javafmllanguage-1.21.1-52.1.0.jar", "d27e1f3149ea08fc0a80779fe24fa8585692c5042a34bad153e55662486acc15"),
    ("eventbus-6.2.27.jar", "d2a7bcecb80b33f90854a003dc295960263d78435d9d6016c427d711510cb999"),
    ("forge-1.21.1-52.1.0-universal.jar", "88ef6625122cfdb9d7b3176bd61b8c0692a191bad0edd77ffddc6a765ee4979c"),
)
ENTRY = "com.example.identitycheck.IdentityProbe"


def fixture(args, run, binary):
    source = REPO / "tests/identity/src"
    libraries = [args.forge_dependencies.resolve() / name for name, _ in FORGE]
    for path, (_, expected) in zip(libraries, FORGE):
        assert w.digest(path) == expected, f"Original Forge dependency mismatch: {path}"
    assert not list(source.rglob("*.class"))
    inputs = w.fingerprints([p for p in source.rglob("*") if p.is_file()] + libraries + [Path(__file__)])
    w.save(run / "inputs.json", inputs)
    shutil.copytree(source, run / "source")
    classes = run / "classes"
    classes.mkdir()
    w.native(run, "javac", [binary / "javac", "--release", "21", "-encoding", "UTF-8",
             "-classpath", os.pathsep.join(map(str, libraries)), "-d", classes,
             *sorted((source / "main/java").rglob("*.java"))], cwd=run)
    root = run / "jar-root"
    shutil.copytree(classes, root)
    shutil.copytree(source / "main/resources", root, dirs_exist_ok=True)
    jar = run / "identity-check.jar"
    w.native(run, "jar", [binary / "jar", "--create", "--file", jar, "--manifest",
             root / "META-INF/MANIFEST.MF", "-C", root, "."], cwd=run)
    w.native(run, "javap-entry", [binary / "javap", "-p", "-v", "-c", "-classpath", jar, ENTRY], cwd=run)
    w.native(run, "javap-event", [binary / "javap", "-p", "-v", "-c", "-classpath", jar,
             "com.example.identitycheck.IdentityEvent"], cwd=run)
    w.unchanged(inputs)
    with zipfile.ZipFile(jar) as archive:
        entry = archive.read(ENTRY.replace(".", "/") + ".class")
        event = archive.read("com/example/identitycheck/IdentityEvent.class")
        assert b"net/minecraftforge/eventbus/api/IEventBus" in entry
        assert b"net/minecraftforge/fml/common/Mod" in entry
        assert b"net/minecraftforge/common/MinecraftForge" in entry
        assert b"net/minecraftforge/eventbus/api/SubscribeEvent" in entry
        assert b"net/minecraftforge/eventbus/api/Event" in event
        assert b"net/neoforged" not in entry + event
    w.save(run / "jar-contents.json", w.jar_contents(jar))
    return {"status": "PASS", "jar": w.fingerprints([jar])[0], "nativeForgeBytecode": True}


def dynamic(args, run, binary):
    compat, mod = args.jar.resolve(), args.fixture.resolve()
    assert compat.name == "forgecompat-002-0l.jar" and mod.name == "identity-check.jar"
    w.server_preflight(args, compat)
    inputs = w.fingerprints([compat, mod, Path(__file__), REPO / "tools/lib/unix_workflow.py"])
    w.save(run / "inputs.json", inputs)
    meta, logs, server = w.harness(args, run, binary, mod, compat, installed_mod_name=mod.name)
    label = "spike002-0l-treatment"
    stdout = (logs / (label + ".stdout.log")).read_text(errors="replace")
    debug = (logs / (label + ".debug.log")).read_text(errors="replace")
    checks = {
        "exact B installed under identity-check.jar": w.digest(server / "mods" / mod.name) == w.digest(mod),
        "exact new compat installed": w.digest(server / "mods" / compat.name) == w.digest(compat),
        "exactly B and compat": sorted(p.name for p in (server / "mods").iterdir()) == sorted([mod.name, compat.name]),
        "identified exactly once": debug.count('Found mod file "identity-check.jar" [locator: forgecompat locator') == 1,
        "real ForgeReader metadata": "ForgeModReader: returning IModFile" in stdout
             and "Found valid mod file identity-check.jar with {identityprobe} mods" in debug,
        "correct modId and entry association": f"loadMod called for identityprobe entrypoints=[{ENTRY}]" in stdout,
        "isolated new transformation service": f"Found additional transformation services from discovery services: [{server / 'mods' / compat.name}]" in debug
             and "Loaded service forgecompat-transform" in debug,
    }
    if args.stage == "before":
        checks.update({
            "no identity transform target offered": "offered: com/example/identitycheck/" not in stdout,
            "no constructor or register marker": "IDENTITY PROBE CONSTRUCTOR REACHED" not in stdout
                and "IDENTITY PROBE REGISTER RETURNED" not in stdout,
            "FML identifies failed class": f"class {ENTRY}" in stdout,
            "direct unsupported Forge constructor argument": "Mod constructor has unsupported argument interface net.minecraftforge.eventbus.api.IEventBus" in stdout,
            "early failure": meta["status"] == "EXITED_EARLY" and meta["doneLine"] is None,
        })
    else:
        markers = ["IDENTITY PROBE CONSTRUCTOR REACHED", "IDENTITY PROBE REGISTER RETURNED",
                   "offered: com/example/identitycheck/IdentityProbe", "offered: com/example/identitycheck/IdentityEvent",
                   "REWRITE hits=1", "post-check ctorCount=1 oldAbsent=true newPresent=true",
                   "API original-pattern hits=1", "API_REWRITE enabled=true hits=1",
                   "SUBSCRIBER enabled=true annotationHits=1", "EVENT_PROFILE enabled=true superHits=1 constructorHits=1",
                   f"register returned target={ENTRY} declaredMethods=1", "delegate -> builtin javafml for neoforge",
                   "NeoForge mod loading, version 21.1.256", "Done (", "Stopping server"]
        checks.update({marker: marker in stdout for marker in markers})
        checks["done stop exit0"] = meta["status"] == "DONE_STOPPED_GRACEFULLY" and meta["exitCode"] == 0
        texts = [stdout, debug, (logs / (label + ".log")).read_text(errors="replace")]
        checks["no ERROR FATAL dump FAIL"] = not any(re.search(r"\b(ERROR|FATAL)\b|dump FAIL", t) for t in texts)
        checks["stderr empty"] = (logs / (label + ".stderr.log")).stat().st_size == 0
        checks["no crash"] = not any(p.is_file() for p in (logs / "crash-reports").rglob("*"))
        checks["nonempty profile uses actual register"] = "delegate to NeoForge register" in stdout and "legacy-empty-noop" not in stdout
        for name in ("IdentityProbe", "IdentityEvent"):
            dump = run / f"dumps/profile/{name}.class"
            checks[f"{name} dump exists"] = dump.is_file()
            if dump.is_file():
                w.native(run, "javap-" + name, [binary / "javap", "-p", "-v", "-c", dump], cwd=run)
        w.save(run / "dump-fingerprints.json", w.fingerprints(list((run / "dumps").rglob("*.class"))))
    w.unchanged(inputs)
    w.save(run / "assertions.json", checks)
    assert all(checks.values()), "See assertions.json and raw logs"
    return {"status": "PASS", "stage": args.stage, "meta": meta, "assertions": checks}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stage", choices=("fixture", "before", "after"), required=True)
    parser.add_argument("--forge-dependencies", type=Path)
    parser.add_argument("--fixture", type=Path)
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--server-input", type=Path, default=Path("/workspace/dualforge-cloud/server-base"))
    parser.add_argument("--jdk-home", type=Path, default=Path(os.environ.get("JAVA_HOME", "/workspace/dualforge-cloud/tools/msjdk/usr/lib/jvm/msopenjdk-21-amd64")))
    parser.add_argument("--java-network-args", type=Path)
    parser.add_argument("--public-key-relay", action="store_true")
    parser.add_argument("--allow-server-start", action="store_true")
    parser.add_argument("--max-wait-seconds", type=int, default=120)
    parser.add_argument("--post-done-seconds", type=int, default=10)
    args = parser.parse_args()
    run = w.run_directory(REPO / "out/convergence-003b" / args.stage)
    w.save(run / "invocation.json", {k: str(v) if isinstance(v, Path) else v for k, v in vars(args).items()})
    result = {"status": "FAIL", "directory": str(run)}
    try:
        binary = w.tools(args, run)
        result.update(fixture(args, run, binary) if args.stage == "fixture" else dynamic(args, run, binary))
    finally:
        w.save(run / "result.json", result)
        print("RESULT", result["status"], run)


if __name__ == "__main__":
    main()
