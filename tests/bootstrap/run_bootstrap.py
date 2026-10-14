"""Stock NeoForge launches with untouched installer startup files and no injected JVM options."""
import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')


def severity_records(text):
    # Compare actual error headers, with time/color removed; do not suppress errors by substring.
    plain = re.sub(r'\x1b\[[0-9;]*m', '', text)
    return dict(Counter(re.findall(r'\[[^\]\n]+/(?:ERROR|FATAL)\] \[[^\]\n]+\]: [^\n]*', plain)))


def run(args):
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    source = args.install.resolve()
    server = output / 'server'
    server.mkdir()
    # Physical copies isolate the official runtime, generated configuration and world.
    shutil.copytree(source / 'libraries', server / 'libraries')
    for name in ('run.sh', 'user_jvm_args.txt'):
        shutil.copyfile(source / name, server / name)
    for line in (source / 'user_jvm_args.txt').read_text().splitlines():
        if line.strip() and not line.lstrip().startswith('#'):
            raise ValueError('Expected installer-default user_jvm_args.txt')
    (server / 'eula.txt').write_text('eula=true\n')
    mods = server / 'mods'
    mods.mkdir()
    for jar in args.mod:
        if (mods / jar.name).exists():
            raise ValueError('Conflicting mod filename: ' + jar.name)
        shutil.copyfile(jar, mods / jar.name)
    tracked = [server / 'run.sh', server / 'user_jvm_args.txt',
               *sorted((server / 'libraries').rglob('*.*')), *sorted(mods.iterdir())]
    records = [{'path': str(p.relative_to(server)), 'sha256': sha(p), 'bytes': p.stat().st_size}
               for p in tracked if p.is_file()]
    save(output / 'installed-inputs.json', records)
    env = os.environ.copy()
    if any(env.get(key) for key in ('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS')):
        raise ValueError('Inherited JVM options are not allowed')
    env['PATH'] = str(args.jdk_home.resolve() / 'bin') + os.pathsep + env['PATH']
    command = ['sh', 'run.sh', 'nogui']
    save(output / 'command.json', {'command': command, 'cwd': str(server),
                                 'java': str(args.jdk_home.resolve() / 'bin/java'),
                                 'zeroDualForgeParameters': True, 'businessProperties': [],
                                 'userJvmArgsSHA256': sha(server / 'user_jvm_args.txt')})
    state, done, seen_java = 'TIMEOUT', False, False
    started = time.monotonic()
    with (output / 'stdout.log').open('wb') as out, (output / 'stderr.log').open('wb') as err:
        process = subprocess.Popen(command, cwd=server, env=env, stdin=subprocess.PIPE, stdout=out, stderr=err)
        try:
            while time.monotonic() - started < args.timeout:
                if not seen_java:
                    for proc in Path('/proc').iterdir():
                        if not proc.name.isdigit():
                            continue
                        try:
                            if (proc / 'exe').resolve().name == 'java' and (proc / 'cwd').resolve() == server:
                                actual = [s.decode() for s in (proc / 'cmdline').read_bytes().split(b'\0') if s]
                                save(output / 'actual-java-command.json', actual)
                                if actual[1:] != ['@user_jvm_args.txt', '@libraries/net/neoforged/neoforge/21.1.256/unix_args.txt', 'nogui']:
                                    raise ValueError('Unexpected actual JVM command')
                                seen_java = True
                        except (FileNotFoundError, PermissionError):
                            pass
                text = (output / 'stdout.log').read_text(errors='replace')
                if 'Done (' in text:
                    done = True
                    time.sleep(3)
                    for console in args.console:
                        process.stdin.write((console + '\n').encode())
                        process.stdin.flush()
                        time.sleep(2)
                    process.stdin.write(b'stop\n')
                    process.stdin.flush()
                    process.wait(timeout=90)
                    state = 'DONE_STOPPED'
                    break
                if process.poll() is not None:
                    state = 'EXITED_BEFORE_DONE'
                    break
                time.sleep(0.5)
        finally:
            if process.poll() is None:
                process.kill()
                process.wait()
    for name in ('logs', 'crash-reports'):
        if (server / name).exists():
            shutil.copytree(server / name, output / name)
    stdout = (output / 'stdout.log').read_text(errors='replace')
    stderr = (output / 'stderr.log').read_text(errors='replace')
    runtime = '\n'.join(p.read_text(errors='replace') for p in (output / 'logs').glob('*.log'))
    severity = severity_records(stdout)
    baseline = json.loads(args.baseline.read_text()) if args.baseline else None
    if baseline and not (baseline['assertions']['DoneStopExit0'] and baseline['zeroDualForgeParameters']):
        raise ValueError('Baseline must be an actual successful stock lifecycle')
    checks = {'stockCommandCaptured': seen_java, 'DoneStopExit0': done and process.returncode == 0
              and 'Stopping server' in stdout, 'stderrEmpty': not stderr,
              'noErrorFatalDumpFail': not re.search(r'\b(?:ERROR|FATAL)\b|dump FAIL', stdout + runtime),
              'noCrash': not any((output / 'crash-reports').glob('*')),
              'noClassDump': not any(server.rglob('*.class')),
              'noWindowsPathArtifacts': not any('D:\\' in p.name for p in server.rglob('*')),
              'inputsUnchanged': all(sha(server / v['path']) == v['sha256'] for v in records)}
    if baseline:
        # The strict result remains visible. Only this new stock-runtime gate accepts exact baseline errors.
        checks['noNewBaselineErrors'] = severity == baseline['severityRecords'] and 'dump FAIL' not in stdout + runtime
    for marker in args.require:
        checks['marker:' + marker] = marker in stdout
    save(output / 'assertions.json', checks)
    effective = {k: v for k, v in checks.items() if not (baseline and k == 'noErrorFatalDumpFail')}
    save(output / 'result.json', {'status': 'PASS' if all(effective.values()) else 'FAIL',
         'state': state, 'exitCode': process.returncode, 'seconds': round(time.monotonic() - started, 2),
         'compatSHA256': next((v['sha256'] for v in records if v['path'] == 'mods/DualForge.jar'), None),
         'zeroDualForgeParameters': True, 'assertions': checks, 'severityRecords': severity,
         'baseline': str(args.baseline.resolve()) if args.baseline else None,
         'strictNoErrors': checks['noErrorFatalDumpFail']})
    # Retain raw logs/results and fingerprints; redundant official dependency copies are excluded.
    if not args.keep_runtime:
        shutil.rmtree(server / 'libraries')
    print(json.dumps(json.loads((output / 'result.json').read_text()), indent=2), flush=True)
    return 0 if all(effective.values()) else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('install', 'output', 'jdk-home'):
        parser.add_argument('--' + key, type=Path, required=True)
    parser.add_argument('--mod', type=Path, action='append', default=[])
    parser.add_argument('--require', action='append', default=[])
    parser.add_argument('--console', action='append', default=[])
    parser.add_argument('--timeout', type=int, default=180)
    parser.add_argument('--keep-runtime', action='store_true')
    parser.add_argument('--baseline', type=Path)
    raise SystemExit(run(parser.parse_args()))
