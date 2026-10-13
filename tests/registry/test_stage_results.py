"""A previous or mislabelled artifact must never produce an aggregate PASS."""
import copy
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("registry_runner", Path(__file__).with_name("run_registry.py"))
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


def cohort():
    compat = {"Path": "/run/dualforge.jar", "SHA256": "a" * 64}
    fixture = {"Path": "/run/registry-probe.jar", "SHA256": "b" * 64}
    driver = {"Path": "/run/registry-driver.jar", "SHA256": "c" * 64}
    dynamic = {"status": "PASS", "compat": compat, "fixture": fixture, "driver": driver,
               "environment": {"hostInputs": "frozen", "java": "pinned", "serverProperties": ["key=value"]},
               "runtimeBytecode": {"compat": compat, "fixture": fixture}}
    return {"contract": {"status": "PASS", "compat": compat, "fixture": fixture},
            "control": copy.deepcopy(dynamic), "treatment": copy.deepcopy(dynamic),
            "regression": {"status": "PASS", "compat": compat},
            "fixture": {"status": "PASS", "jars": {"registry-probe": fixture, "registry-driver": driver}},
            "assets": {"status": "PASS", "fixture": fixture}}


class BindingTest(unittest.TestCase):
    def test_same_cohort(self):
        self.assertTrue(runner.cohort_consistent(cohort()))

    def test_every_compat_stage_mismatch(self):
        for stage in ("contract", "control", "treatment", "regression"):
            with self.subTest(stage=stage):
                result = copy.deepcopy(cohort())
                result[stage]["compat"] = {"Path": "/other/dualforge.jar", "SHA256": "d" * 64}
                self.assertFalse(runner.cohort_consistent(result))

    def test_missing_hash(self):
        result = cohort()
        result["contract"]["compat"] = {"Path": "/run/dualforge.jar"}
        self.assertFalse(runner.cohort_consistent(result))

    def test_sorted_fingerprint_role_confusion(self):
        result = cohort()
        for stage in ("contract", "control", "treatment", "regression"):
            result[stage]["compat"] = result["treatment"]["driver"]
        self.assertFalse(runner.cohort_consistent(result))

    def test_fixture_driver_runtime_and_assets_mismatch(self):
        for stage, role in (("contract", "fixture"), ("control", "fixture"), ("treatment", "driver"), ("assets", "fixture")):
            with self.subTest(stage=stage, role=role):
                result = copy.deepcopy(cohort())
                result[stage][role] = {"Path": "/stale/input.jar", "SHA256": "d" * 64}
                self.assertFalse(runner.cohort_consistent(result))
        result = copy.deepcopy(cohort())
        result["control"]["runtimeBytecode"]["compat"] = {"SHA256": "e" * 64}
        self.assertFalse(runner.cohort_consistent(result))

    def test_host_or_java_mismatch(self):
        for key in ("hostInputs", "java", "serverProperties"):
            result = copy.deepcopy(cohort())
            result["treatment"]["environment"][key] = "other"
            self.assertFalse(runner.cohort_consistent(result))


if __name__ == "__main__":
    unittest.main()
