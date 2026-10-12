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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stage", choices=("fixture", "baseline"), required=True)
    for name in ("forge-dependencies", "fixture", "driver", "jar", "java-network-args"):
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
        required = {"fixture": ("forge_dependencies",), "baseline": ("jar", "fixture", "driver")}[args.stage]
        for name in required:
            if getattr(args, name) is None or not getattr(args, name).exists():
                raise w.NotRunError("Missing explicit input: " + name)
        binary = w.tools(args, run)
        result.update(globals()[args.stage](args, run, binary))
    except Exception as error:
        result.update(status="NOT RUN" if isinstance(error, w.NotRunError) else "FAIL", error=str(error))
        (run / "failure.txt").write_text(traceback.format_exc())
        raise
    finally:
        w.save(run / "result.json", result)
        print("RESULT", result["status"], run)


if __name__ == "__main__":
    main()
