"""Serial FML scan and byte preservation controls for Convergence-003b."""
import argparse
import os
from pathlib import Path
import shutil
import zipfile
from run_identity import REPO, FORGE, w


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--fixture", type=Path, required=True)
    parser.add_argument("--forge-dependencies", type=Path, required=True)
    parser.add_argument("--server-input", type=Path, default=REPO / "runtime/neoforge-server")
    parser.add_argument("--jdk-home", type=Path, default=Path(os.environ.get("JAVA_HOME", "/workspace/dualforge-cloud/tools/msjdk/usr/lib/jvm/msopenjdk-21-amd64")))
    args = parser.parse_args()
    run = w.run_directory(REPO / "out/convergence-003b/scope")
    result = {"status": "FAIL", "directory": str(run)}
    try:
        binary = w.tools(args, run)
        libs = sorted((args.server_input.resolve() / "libraries").rglob("*.jar"))
        forge = [args.forge_dependencies.resolve() / name for name, _ in FORGE]
        for path, (_, expected) in zip(forge, FORGE):
            assert w.digest(path) == expected
        source = REPO / "tests/identity"
        inputs = w.fingerprints([args.jar.resolve(), args.fixture.resolve(), *forge, *libs,
                                *source.glob("*.py"), source / "IdentityScopeTest.java", *source.glob("controls/*.java")])
        w.save(run / "inputs.json", inputs)
        classes = run / "classes"
        classes.mkdir()
        w.native(run, "javac-controls", [binary / "javac", "--release", "21", "-encoding", "UTF-8",
                 "-classpath", os.pathsep.join(map(str, [*forge, *libs])), "-d", classes,
                 *sorted(source.glob("controls/*.java"))], cwd=run)
        root = run / "jar-root"
        shutil.copytree(classes, root)
        with zipfile.ZipFile(args.fixture.resolve()) as archive:
            archive.extractall(root)
        controlled = run / "scope-forge.jar"
        w.native(run, "jar-forge", [binary / "jar", "--create", "--file", controlled,
                 "--manifest", root / "META-INF/MANIFEST.MF", "-C", root, "."], cwd=run)
        # Same classes and matching modId, but native metadata is authoritative.
        toml = (root / "META-INF/mods.toml").read_text().replace('[52,)', '[3,)')
        (root / "META-INF/neoforge.mods.toml").write_text(toml)
        native = run / "scope-native-dual.jar"
        w.native(run, "jar-native", [binary / "jar", "--create", "--file", native,
                 "--manifest", root / "META-INF/MANIFEST.MF", "-C", root, "."], cwd=run)
        w.save(run / "fixture-fingerprints.json", w.fingerprints([controlled, native]))
        w.save(run / "fixture-contents.json", {"forge": w.jar_contents(controlled), "native": w.jar_contents(native)})
        cp = os.pathsep.join(map(str, [args.jar.resolve(), *libs]))
        test = run / "test-classes"
        test.mkdir()
        w.native(run, "javac-test", [binary / "javac", "--release", "21", "-encoding", "UTF-8",
                 "-classpath", cp, "-d", test, source / "IdentityScopeTest.java"], cwd=run)
        dumps = run / "bytes"
        w.native(run, "scope", [binary / "java", "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
                 "--add-exports", "java.base/sun.security.util=ALL-UNNAMED", *w.FLAGS,
                 f"-Ddualforge.dump.path={run / 'dumps/entry.class'}",
                 f"-Ddualforge.dump.profileDir={run / 'dumps/profile'}",
                 "-classpath", str(test) + os.pathsep + cp, "io.github.gfander.dualforge.IdentityScopeTest",
                 controlled, native, dumps], cwd=run)
        assert "PASS identity scope controls" in (run / "scope.stdout.log").read_text()
        assert (run / "scope.stderr.log").stat().st_size == 0
        w.unchanged(inputs)
        w.save(run / "byte-fingerprints.json", w.fingerprints(list(dumps.rglob("*.class"))))
        result.update(status="PASS", directAssertions="scope.stdout.log", bytes="byte-fingerprints.json")
    finally:
        w.save(run / "result.json", result)
        print("RESULT", result["status"], run)


if __name__ == "__main__":
    main()
