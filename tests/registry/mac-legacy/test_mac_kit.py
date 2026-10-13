"""Linux isolated Zsh behavioral tests with MOCKED Mac/Prism/Java metadata; not Mac execution."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import unittest
import uuid
import zipfile

SOURCE = Path(__file__).resolve().parent
REPO = SOURCE.parents[2]
spec = importlib.util.spec_from_file_location("mac_builder", SOURCE / "build_kit.py")
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)
COMPAT = REPO / "out/convergence-005/implementation-build/20261012-165349-ad26da3e/forgecompat-002-0l.jar"
FIXTURE = REPO / "out/convergence-005/fixture/20261012-192217-d17bd31f/registry-probe.jar"


def snapshot(folder):
    return {str(p.relative_to(folder)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in folder.rglob("*") if p.is_file() and not p.is_symlink()}


class MacScriptTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.root = REPO / "out/convergence-005/mac-client-kit/tests" / uuid.uuid4().hex[:8]
        builder.build(argparse.Namespace(compat=COMPAT, fixture=FIXTURE, output_dir=cls.root / "package"))
        cls.base_kit = cls.root / "package/dualforge-005-mac-client-kit"
        cls.mock = cls.root / "mock-bin"
        cls.mock.mkdir()
        commands = {
            "uname": '#!/bin/sh\ncase "$1" in -s) echo Darwin;; -m) echo arm64;; esac\n',
            "sw_vers": '#!/bin/sh\necho 15.0\n',
            "prismlauncher": '#!/bin/sh\nexit 0\n',
            "java-mock": '#!/bin/sh\necho "    java.version = 21.0.10" >&2\necho "    os.arch = aarch64" >&2\n',
            # Match the reported Mac: JSON works with -p/-extract, not plist -lint.
            "plutil": '#!/usr/bin/env python3\nimport json,sys\na=sys.argv[1:]\nif a[0]=="-lint":sys.stderr.write("Unexpected character { at line 1\\n");sys.exit(1)\ntry:v=json.load(open(a[-1]))\nexcept (ValueError,OSError):sys.exit(1)\nif a[0]=="-p":print(v);sys.exit(0)\ntry:\n for k in a[1].split("."):v=v[int(k)] if isinstance(v,list) else v[k]\n print(v)\nexcept (KeyError,IndexError):sys.exit(1)\n',
        }
        for name, text in commands.items():
            path = cls.mock / name
            path.write_text(text)
            path.chmod(0o755)
        cls.results = []

    @classmethod
    def tearDownClass(cls):
        (cls.root / "commands.json").write_text(json.dumps(cls.results, indent=2) + "\n")
        (cls.root / "status.json").write_text(json.dumps({"scope": "Linux Zsh with mocked Mac/Prism/Java", "MacExecuted": False}, indent=2) + "\n")

    def setUp(self):
        self.work = self.root / self._testMethodName
        self.work.mkdir()
        self.kit = self.work / "kit with spaces"
        shutil.copytree(self.base_kit, self.kit)
        self.instance = self.work / "Prism instance with spaces"
        self.game = self.instance / ".minecraft"
        self.game.mkdir(parents=True)
        self.mods = self.game / "mods"
        self.mods.mkdir()
        (self.game / "world-do-not-touch").write_text("world sentinel")
        (self.instance / "mmc-pack.json").write_text(json.dumps({"components": [
            {"uid": "net.minecraft", "version": "1.21.1"}, {"uid": "net.neoforged", "version": "21.1.256"}]}))
        self.cfg = self.instance / "instance.cfg"
        self.cfg.write_text("OverrideJavaLocation=true\nJavaPath=" + str(self.mock / "java-mock")
                           + "\nOverrideJavaArgs=true\nJvmArgs=" + " ".join((self.kit / "JVM-ARGS.txt").read_text().splitlines()) + "\n")
        self.env = dict(os.environ, PATH=str(self.mock) + os.pathsep + os.environ["PATH"])

    def run_script(self, script, *args, ok=True, native=False):
        cmd = ["zsh", str(self.kit / "scripts" / script), "--instance-dir", str(self.instance), *map(str, args)]
        result = subprocess.run(cmd, env=None if native else self.env, text=True, capture_output=True)
        self.results.append({"command": cmd, "exit": result.returncode, "stdout": result.stdout, "stderr": result.stderr, "mocked": not native})
        self.assertEqual(result.returncode == 0, ok, result.stdout + result.stderr)
        return result

    def test_linux_is_rejected(self):
        self.run_script("mac-preflight.zsh", ok=False, native=True)

    def test_json_uses_parse_mode_not_plist_lint(self):
        self.run_script("mac-preflight.zsh")
        (self.instance / "mmc-pack.json").write_text('{"components": invalid}')
        before = snapshot(self.instance)
        self.run_script("mac-preflight.zsh", ok=False)
        self.assertEqual(before, snapshot(self.instance))

    def test_prism_outer_quoted_jvm_args(self):
        original = self.cfg.read_text()
        flags = "  ".join((self.kit / "JVM-ARGS.txt").read_text().splitlines())
        for arguments in (flags, '"' + flags + '"',
                          '"' + flags + '  \\"-Dlabel=two words\\""'):
            self.cfg.write_text(original.split("JvmArgs=", 1)[0] + "JvmArgs=" + arguments + "\n")
            # Isolate JVM parsing from the separate JSON/plutil failure.
            command = ["zsh", "-c", 'source "$1"; DF_INSTANCE="$2"; DF_GAME="$3"; DF_KIT="$4"; df_jvm',
                       "jvm-parser-test", str(self.kit / "scripts/mac-common.zsh"), str(self.instance), str(self.game), str(self.kit)]
            result = subprocess.run(command, text=True, capture_output=True)
            self.results.append({"command": command, "exit": result.returncode, "stdout": result.stdout,
                                 "stderr": result.stderr, "mocked": True})
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_phase_b_c_dry_run_ownership_rollback(self):
        before = snapshot(self.instance)
        self.run_script("mac-install.zsh", "--install", "--phase", "B", ok=False)
        self.run_script("mac-install.zsh", "--dry-run", "--phase", "B")
        self.assertEqual(before, snapshot(self.instance))
        self.run_script("mac-install.zsh", "--install", "--phase", "B")
        self.assertFalse((self.mods / "registry-probe.jar").exists())
        self.run_script("mac-install.zsh", "--dry-run", "--phase", "C")
        self.run_script("mac-install.zsh", "--install", "--phase", "C")
        self.assertEqual((self.mods / "registry-probe.jar").read_bytes(), FIXTURE.read_bytes())
        (self.mods / "user-added.jar").write_text("do not delete")
        self.run_script("mac-install.zsh", "--rollback")
        self.assertEqual((self.mods / "user-added.jar").read_text(), "do not delete")
        self.assertEqual((self.game / "world-do-not-touch").read_text(), "world sentinel")
        self.assertFalse((self.mods / "forgecompat-002-0l.jar").exists())

    def test_sha_ledger_validation_and_legacy_rollback(self):
        self.run_script("mac-install.zsh", "--dry-run")
        self.run_script("mac-install.zsh", "--install")
        ledger = self.mods / ".dualforge-005-owned"
        entries = [line.split() for line in ledger.read_text().splitlines()]
        self.assertEqual(len(entries), 2)
        for name, digest in entries:
            self.assertEqual(digest, hashlib.sha256((self.kit / "mods" / name).read_bytes()).hexdigest())
        ledger.write_text(entries[0][0] + " " + "0" * 64 + "\n")
        before = snapshot(self.instance)
        self.run_script("mac-install.zsh", "--rollback", ok=False)
        self.assertEqual(before, snapshot(self.instance))
        # Existing users can roll back their original filename-only receipt safely.
        ledger.write_text("\n".join(name for name, _ in entries) + "\n")
        self.run_script("mac-install.zsh", "--rollback")
        self.assertFalse((self.mods / "registry-probe.jar").exists())
        self.assertEqual((self.game / "world-do-not-touch").read_text(), "world sentinel")

    def test_rollback_rejects_changed_owned_artifact_atomically(self):
        self.run_script("mac-install.zsh", "--dry-run")
        self.run_script("mac-install.zsh", "--install")
        with (self.mods / "registry-probe.jar").open("ab") as stream:
            stream.write(b"changed")
        before = snapshot(self.instance)
        self.run_script("mac-install.zsh", "--rollback", ok=False)
        self.assertEqual(before, snapshot(self.instance))

    def test_same_named_different_sha_and_other_mods_refused(self):
        for name in ("other.jar", "forgecompat-002-0l.jar"):
            file = self.mods / name
            file.write_bytes(b"user input")
            before = snapshot(self.instance)
            self.run_script("mac-install.zsh", "--dry-run", ok=False)
            self.assertEqual(before, snapshot(self.instance))
            file.unlink()

    def test_preexisting_correct_jar_survives_rollback(self):
        shutil.copyfile(COMPAT, self.mods / "forgecompat-002-0l.jar")
        self.run_script("mac-install.zsh", "--dry-run", "--phase", "B")
        self.run_script("mac-install.zsh", "--install", "--phase", "B")
        self.run_script("mac-install.zsh", "--rollback")
        self.assertEqual((self.mods / "forgecompat-002-0l.jar").read_bytes(), COMPAT.read_bytes())

    def test_wrong_versions_missing_flags_and_kit_tamper(self):
        pack = self.instance / "mmc-pack.json"
        original = pack.read_text()
        pack.write_text(original.replace("21.1.256", "21.1.255"))
        self.run_script("mac-preflight.zsh", ok=False)
        pack.write_text(original)
        self.cfg.write_text(self.cfg.read_text().replace("-DDF005_REGISTER=true", "-DDF005_REGISTER=false"))
        self.run_script("mac-preflight.zsh", ok=False)
        self.cfg.write_text(self.cfg.read_text().replace("-DDF005_REGISTER=false", "-DDF005_REGISTER=true"))
        self.run_script("mac-preflight.zsh")
        (self.kit / "JVM-ARGS.txt").write_text("tampered")
        self.run_script("mac-preflight.zsh", ok=False)

    def test_linked_mods_refused(self):
        self.mods.rmdir()
        other = self.work / "external"
        other.mkdir()
        self.mods.symlink_to(other, target_is_directory=True)
        self.run_script("mac-install.zsh", "--dry-run", ok=False)
        self.assertEqual(list(other.iterdir()), [])

    def test_intel_java21_wrong_major_and_conflicting_flags(self):
        local_bin = self.work / "intel-bin"
        local_bin.mkdir()
        uname = local_bin / "uname"
        uname.write_text('#!/bin/sh\ncase "$1" in -s) echo Darwin;; -m) echo x86_64;; esac\n')
        uname.chmod(0o755)
        java = local_bin / "java-intel"
        java.write_text('#!/bin/sh\necho "    java.version = 21.0.10" >&2\necho "    os.arch = amd64" >&2\n')
        java.chmod(0o755)
        self.cfg.write_text(self.cfg.read_text().replace(str(self.mock / "java-mock"), str(java)))
        self.env["PATH"] = str(local_bin) + os.pathsep + self.env["PATH"]
        self.run_script("mac-preflight.zsh")
        java.write_text(java.read_text().replace("21.0.10", "17.0.1"))
        self.run_script("mac-preflight.zsh", ok=False)
        java.write_text(java.read_text().replace("17.0.1", "21.0.10"))
        self.cfg.write_text(self.cfg.read_text().replace("-DDF005_REGISTER=true", "-DDF005_REGISTER=true -DDF005_REGISTER=false"))
        self.run_script("mac-preflight.zsh", ok=False)

    def test_quoted_properties_remain_strict_and_do_not_execute(self):
        initial = self.cfg.read_text().split("JvmArgs=", 1)[0]
        flags = "  ".join((self.kit / "JVM-ARGS.txt").read_text().splitlines())
        for suffix in (" -DDF005_REGISTER=false", " -DDF005_REGISTER=true"):
            self.cfg.write_text(initial + 'JvmArgs="' + flags + suffix + '"\n')
            self.run_script("mac-preflight.zsh", ok=False)
        marker = self.work / "must-not-execute"
        # Keep command-substitution-looking data literal throughout INI/token parsing.
        self.cfg.write_text(initial + 'JvmArgs="' + flags + " '-Dliteral=$(touch " + str(marker) + ")'\"\n")
        self.run_script("mac-preflight.zsh")
        self.assertFalse(marker.exists())

    def test_collection_redacts_and_keeps_originals(self):
        marker = self.work / "start-marker"
        marker.write_text("")
        os.utime(marker, (time.time() - 60, time.time() - 60))
        logs = self.game / "logs"
        logs.mkdir()
        raw = "[DF005] CONSTRUCTOR REACHED\naccessToken=SECRET\nsession: PRIVATESESSION\npassword: PWD\nConnecting to hidden.example.net\naddress 10.2.3.4\nIPv6 ::1 and 2001:db8:85a3:0:0:8a2e:370:7334\nprivate.example.net\n" + str(self.instance / "mods/registry-probe.jar") + "\n"
        (logs / "latest.log").write_text(raw)
        before = snapshot(self.instance)
        self.run_script("mac-collect-logs.zsh", "--stage", "C", "--since-marker", marker,
                        "--output-dir", self.work, "--redact-text", "private.example.net")
        self.assertEqual(before, snapshot(self.instance))
        diagnostics = next(self.work.glob("dualforge-005-client-C-*.zip"))
        with zipfile.ZipFile(diagnostics) as archive:
            self.assertTrue(any(n.endswith("/checklist/CHECKLIST.md") for n in archive.namelist()))
            text = "\n".join(archive.read(n).decode() for n in archive.namelist() if n.endswith(".txt") or n.endswith(".log"))
        for secret in ("SECRET", "PRIVATESESSION", "PWD", "hidden.example.net", "10.2.3.4", "::1", "2001:db8", "private.example.net"):
            self.assertNotIn(secret, text)
        self.assertIn("[DF005] CONSTRUCTOR REACHED", text)
        self.assertIn("<INSTANCE>/mods/registry-probe.jar", text)
        self.assertNotIn(str(self.instance), text)
        self.assertIn("not runtime log evidence", text)


if __name__ == "__main__":
    unittest.main()
