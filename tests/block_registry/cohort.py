"""Use the existing strict registry cohort rules; no weaker Block-specific aggregate."""
import importlib.util
from pathlib import Path
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('multi_cohort',REPO/'tests/multi_registry/run_multi_registry.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
consistent=module.consistent
