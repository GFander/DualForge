"""Record one stock FnF experiment; a successful runner never implies mod functionality."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[4]
FNF_SHA = 'bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958'
COMPAT_SHA = 'a26893598d1ffdb73078373b007a4d5d54dddd1b774cb2a0fa0dd90fe5602bf7'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--host', choices=['forge', 'neoforge'], required=True)
    for name in ['install', 'jdk-home', 'output']:
        p.add_argument('--' + name, type=Path, required=True)
    for name in ['fnf', 'compat', 'native', 'observer']:
        p.add_argument('--' + name, type=Path)
    a = p.parse_args()
    if a.fnf and sha(a.fnf) != FNF_SHA:
        p.error('Original FnF digest mismatch')
    if a.compat and (a.host != 'neoforge' or sha(a.compat) != COMPAT_SHA):
        p.error('Expected unchanged final 4C-3 NeoForge compat')
    out = a.output.resolve()
    if not out.is_relative_to(ROOT / 'out'):
        p.error('Output must be below this worktree out/')
    out.mkdir(parents=True, exist_ok=False)
    mods = [v.resolve() for v in [a.compat, a.fnf, a.native, a.observer] if v]
    records = [{'path': str(v), 'bytes': v.stat().st_size, 'sha256': sha(v)} for v in mods]
    command = [sys.executable, '-B', str(ROOT / 'tests/real_mods/food_n_fuel/run_stock.py'),
               '--host', a.host, '--install', str(a.install.resolve()), '--jdk-home',
               str(a.jdk_home.resolve()), '--output', str(out / 'stock'), '--timeout', '360']
    for mod in mods:
        command += ['--mod', str(mod)]
    command += ['--console', 'forge mods' if a.host == 'forge' else 'neoforge mods']
    invocation = {'argv': command, 'cwd': str(ROOT), 'sourceCommit': subprocess.check_output(
        ['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
        'runnerSHA256': sha(Path(__file__)), 'installed': records,
        'observerRun': bool(a.observer), 'businessEnvironmentChanges': {}}
    (out / 'command.json').write_text(json.dumps(invocation, indent=2) + '\n')
    with (out / 'runner.stdout.log').open('wb') as stdout, (out / 'runner.stderr.log').open('wb') as stderr:
        r = subprocess.run(command, cwd=ROOT, stdout=stdout, stderr=stderr)
    raw = json.loads((out / 'stock/result.json').read_text()) if (out / 'stock/result.json').exists() else None
    unchanged = all(sha(Path(v['path'])) == v['sha256'] for v in records)
    result = {'runnerExitCode': r.returncode, 'hostLifecycle': raw['lifecycle'] if raw else 'UNKNOWN',
              'inputFilesUnchanged': unchanged, 'modFunctionality': 'NOT ASSERTED',
              'observerRun': bool(a.observer), 'installed': records}
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2), flush=True)
    return 0 if r.returncode == 0 and unchanged else 1


if __name__ == '__main__':
    raise SystemExit(main())
