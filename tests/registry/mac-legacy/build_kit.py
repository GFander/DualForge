"""Cloud-only packaging of the exact previously tested 005-S cohort; no JVM/build/network."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import zipfile

SOURCE = Path(__file__).resolve().parent
REPO = SOURCE.parents[2]
EXPECTED = {
    "forgecompat-002-0l.jar": "ed572ca9762e235e7d264355d35b3c9d3ab9ecf1da3f2c3234b1e28056aad752",
    "registry-probe.jar": "312a4f697c1dc617ed48822c10179384b6559f4e6399db9210c15541c774132c",
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build(args):
    output = args.output_dir.resolve()
    assert output.is_relative_to(REPO / "out"), "Use a fresh ignored checkout out/ directory"
    output.mkdir(parents=True, exist_ok=False)
    root = output / "dualforge-005-mac-client-kit"
    (root / "mods").mkdir(parents=True)
    files = (args.compat.resolve(), args.fixture.resolve())
    records = {}
    for path, name in zip(files, EXPECTED):
        assert path.is_file() and not path.is_symlink() and sha(path) == EXPECTED[name], f"Not the tested cohort: {path}"
        with zipfile.ZipFile(path) as archive:
            assert archive.testzip() is None
        shutil.copyfile(path, root / "mods" / name)
        records[name] = {"SHA256": sha(path), "bytes": path.stat().st_size}
    resources = {}
    with zipfile.ZipFile(files[1]) as archive:
        assert b'modId="registryprobe"' in archive.read("META-INF/mods.toml")
        assert b'loaderVersion="[52,)"' in archive.read("META-INF/mods.toml")
        assert b'Automatic-Module-Name: registryprobe' in archive.read("META-INF/MANIFEST.MF")
        entry = archive.read("com/example/registryprobe/RegistryProbe.class")
        assert b"net/neoforged/" not in entry and b"net/minecraftforge/registries/" in entry
        for name in ("textures/item/ice_ether.png", "models/item/ice_ether.json", "lang/en_us.json", "lang/zh_cn.json"):
            resources[name] = hashlib.sha256(archive.read("assets/registryprobe/" + name)).hexdigest()
        assert resources["textures/item/ice_ether.png"] == "2c810e0c1cca3c451a536a4cf1d72448a5809f999933acc45ca01240315f1681"
    for name in ("MAC-SETUP.md", "JVM-ARGS.txt", "CHECKLIST.md"):
        shutil.copyfile(SOURCE / name, root / name)
    shutil.copytree(SOURCE / "scripts", root / "scripts")
    manifest = {"schemaVersion": 1, "task": "Convergence-005-C", "clientStatus": "PASS",
                "clientScope": "user-run macOS 27.0 arm64 / Microsoft Java 21.0.7+6; original tested JAR cohort",
                "clientEvidence": "Dev Manuals/Convergence-005-C 客户端验收与脚本修复.md: real A/B/C logs/screenshots plus user manual give/English/Chinese confirmation",
                "repairedScriptsMacExecuted": False,
                "serverStatus": "PASS", "minecraft": "1.21.1", "neoforge": "21.1.256", "javaMajor": 21,
                "platform": "macOS", "architectures": ["arm64", "x86_64"],
                "sourceCommit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=REPO, text=True).strip(),
                "productionCommit": "fc15b6231b62523392bd0d888bf888dbb16ccb4a",
                "fixtureSourceCommit": "fe0716640a6bac8d71a4a33ba4874d925e616976",
                "cohort": "original retained tested 005-S; both JAR files byte-identical; no new build",
                "mods": records, "resources": resources, "textureDimensions": [16, 16],
                "jvmArgs": (root / "JVM-ARGS.txt").read_text().splitlines(),
                "nativeForgeCompilation": {"Forge": "52.1.0", "bus": "6.2.27", "noNeoForgeOrCompatClasspath": True,
                    "evidence": "Dev Manuals/Convergence-005 证据索引.json: original native-compile-inputs, source snapshots and purity assertions"},
                "testDriverIncluded": False, "downloadUrl": None,
                "files": {str(p.relative_to(root)): {"SHA256": sha(p), "purpose": "tested platform-independent mod" if p.suffix == ".jar" else "manual setup/checklist/JVM arguments or Mac deployment validation"}
                          for p in sorted(root.rglob("*")) if p.is_file()}}
    (root / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
    payload = sorted(p for p in root.rglob("*") if p.is_file())
    (root / "SHA256SUMS").write_text("".join(f"{sha(p)}  {p.relative_to(root)}\n" for p in payload))
    archive = output / "dualforge-005-mac-client-kit.zip"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as package:
        for path in sorted(p for p in root.rglob("*") if p.is_file()):
            package.write(path, str(path.relative_to(output)))
    with zipfile.ZipFile(archive) as package:
        assert package.testzip() is None
        assert len(package.namelist()) == len(payload) + 1
        for name, expected in EXPECTED.items():
            assert hashlib.sha256(package.read(root.name + "/mods/" + name)).hexdigest() == expected
        assert all(not any(x in n for x in ("libraries/", "world/", "accounts", "driver.jar")) for n in package.namelist())
    result = {"status": "PASS", "sourceCommit": manifest["sourceCommit"], "zip": str(archive),
              "zipSHA256": sha(archive), "zipBytes": archive.stat().st_size, "contents": len(payload) + 1,
              "mods": records, "resources": resources, "005-S": "PASS", "005-C": "PASS",
              "clientScope": manifest["clientScope"], "repairedScriptsMacExecuted": False,
              "uploaded": False, "downloadUrl": None, "CloudMacExecuted": False}
    (output / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    (output / (archive.name + ".sha256")).write_text(f"{sha(archive)}  {archive.name}\n")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--compat", type=Path, required=True)
    parser.add_argument("--fixture", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    build(parser.parse_args())
