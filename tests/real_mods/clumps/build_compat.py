"""Use the maintained production builder with a fresh SSD output directory."""
import argparse
import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location('workflow', ROOT / 'tools/lib/unix_workflow.py')
WORKFLOW = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(WORKFLOW)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['server-input', 'jdk-home', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    if not args.output.resolve().is_relative_to((Path.home() / 'Env/DualForge/runs').resolve()):
        parser.error('Build output must be below ~/Env/DualForge/runs')
    args.output.mkdir(parents=True, exist_ok=False)
    binary = WORKFLOW.tools(args, args.output)
    jar, result = WORKFLOW.build(args, args.output, binary)
    WORKFLOW.save(args.output / 'result.json', result)
    print(jar)


if __name__ == '__main__':
    main()
