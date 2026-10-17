"""Keep unchanged legacy contract logic but place its JVM working directory on SSD.

Every override records requested and actual cwd. No game/server runner is wrapped.
"""
import argparse
import importlib.util
from pathlib import Path
import subprocess
import sys
from support import fresh, record, save


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--script', type=Path, required=True)
    parser.add_argument('arguments', nargs=argparse.REMAINDER)
    args = parser.parse_args()
    arguments = args.arguments[1:] if args.arguments[:1] == ['--'] else args.arguments
    destination = Path(arguments[arguments.index('--output') + 1]).resolve()
    if not destination.is_relative_to(Path('/home/albert/Env/DualForge/runs')):
        parser.error('Contract output must be on SSD')
    calls = []
    original = subprocess.run

    def checked(argv, *positional, **kwargs):
        if Path(str(argv[0])).name in ['java', 'javac', 'jar']:
            directory = destination / 'jvm-cwd'
            directory.mkdir(parents=True, exist_ok=True)
            calls.append({'argv': list(map(str, argv)), 'requestedCwd': str(kwargs.get('cwd', Path.cwd())), 'actualCwd': str(directory)})
            kwargs['cwd'] = directory
            save(destination / 'actual-jvm-cwd.json', {'scope': 'Only working-directory adaptation; unchanged contract logic/args',
                 'wrapper': record(Path(__file__)), 'originalTool': record(args.script), 'calls': calls})
        return original(argv, *positional, **kwargs)

    spec = importlib.util.spec_from_file_location('original_contract', args.script)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    sys.argv = [str(args.script), *arguments]
    subprocess.run = checked
    try:
        module.main()
    finally:
        subprocess.run = original


if __name__ == '__main__':
    main()
