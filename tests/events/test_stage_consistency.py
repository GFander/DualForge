"""Metadata-only tests: synthetic rounds must never combine into Convergence-004 PASS."""
import copy
import hashlib
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

import run_events as events


class StageConsistencyTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.root_patch = patch.object(events, "ROOT", self.root)
        self.root_patch.start()
        self.addCleanup(self.root_patch.stop)
        (self.root / "preflight").mkdir()
        events.w.save(self.root / "preflight/git.json", {"baseCommit": "a" * 40})
        self.compat = self.root / "forgecompat-002-0l.jar"
        self.compat.write_bytes(b"synthetic round A; not a JVM artifact")
        self.record = events.w.fingerprints([self.compat])[0]
        self.other_hash = hashlib.sha256(b"synthetic round B").hexdigest()
        self.results = {}
        self.runs = {}
        for stage in ("fixture", "contract", "control", "treatment", "regression"):
            run = self.root / stage
            run.mkdir()
            result = {"status": "PASS"}
            if stage != "fixture":
                result["compat"] = copy.deepcopy(self.record)
            if stage in ("control", "treatment"):
                events.w.save(run / "artifacts.json", [self.record,
                    {"Path": str(self.root / "dispatch-probe.jar"), "SHA256": "b" * 64},
                    {"Path": str(self.root / "dispatch-driver.jar"), "SHA256": "c" * 64}])
            self.results[stage], self.runs[stage] = result, run

    def publish(self, omit=()):
        for stage, result in self.results.items():
            if stage in omit:
                continue
            run = self.runs[stage]
            events.w.save(run / "result.json", result)
            events.publish_result(stage, run, result)
        return json.loads((self.root / "summary.json").read_text())

    def test_same_build_passes(self):
        self.assertEqual(self.publish()["status"], "PASS")

    def test_each_stage_mismatch_fails(self):
        for stage in ("contract", "control", "treatment", "regression"):
            with self.subTest(stage=stage):
                self.results[stage]["compat"]["SHA256"] = self.other_hash
                self.assertEqual(self.publish()["status"], "FAIL")
                self.results[stage]["compat"]["SHA256"] = self.record["SHA256"]

    def test_old_results_without_fingerprint_fail(self):
        del self.results["contract"]["compat"]
        self.assertEqual(self.publish()["status"], "FAIL")

    def test_matching_ct_from_another_round_fails(self):
        for stage in ("control", "treatment"):
            self.results[stage]["compat"]["SHA256"] = self.other_hash
            rows = json.loads((self.runs[stage] / "artifacts.json").read_text())
            rows[0]["SHA256"] = self.other_hash
            events.w.save(self.runs[stage] / "artifacts.json", rows)
        self.assertEqual(self.publish()["status"], "FAIL")

    def test_ct_artifacts_must_match_stage_fingerprints(self):
        for stage in ("control", "treatment"):
            rows = json.loads((self.runs[stage] / "artifacts.json").read_text())
            rows[0]["SHA256"] = self.other_hash
            events.w.save(self.runs[stage] / "artifacts.json", rows)
        self.assertEqual(self.publish()["status"], "FAIL")

    def test_missing_stage_is_partial(self):
        self.assertEqual(self.publish(omit=("regression",))["status"], "PARTIAL")

    def test_child_regression_mismatch_is_rejected(self):
        run = self.root / "regression-execution"
        run.mkdir()
        events.w.save(self.root / "preflight/inputs-before.json", [])
        args = SimpleNamespace(java_network_args=None, public_key_relay=False,
            server_input=self.root, jar=self.compat, probe=self.root / "probe.jar",
            identity_fixture=self.root / "identity-check.jar", forge_dependencies=self.root,
            allow_server_start=True)
        real_digest = events.w.digest

        def digest(path):
            if path == args.identity_fixture:
                return "95ad77b0c2f5005bd7e98af139a911559635d4dd55e25463052a463748161b22"
            return real_digest(path)

        def child(directory, name, command, cwd):
            output = self.root / ("child-" + name)
            output.mkdir()
            record = copy.deepcopy(self.record)
            if name == "scope":
                record["SHA256"] = self.other_hash
            events.w.save(output / "result.json", {"status": "PASS"})
            events.w.save(output / ("harness-inputs.json" if name == "original-smoke" else "inputs.json"), [record])
            (directory / (name + ".stdout.log")).write_text("RESULT PASS " + str(output) + "\n")

        with patch.object(events.w, "native", side_effect=child), patch.object(events.w, "digest", side_effect=digest):
            with self.assertRaisesRegex(RuntimeError, "scope.*compat"):
                events.regression(args, run, None)


if __name__ == "__main__":
    unittest.main()
