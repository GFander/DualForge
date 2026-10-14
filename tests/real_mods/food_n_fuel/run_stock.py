"""Record stock Forge/NeoForge lifecycles; this runner never declares mod functionality PASS."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import signal
import socket
import subprocess
import time

REPO = Path(__file__).resolve().parents[3]
spec = importlib.util.spec_from_file_location('bootstrap', REPO / 'tests/bootstrap/run_bootstrap.py')
bootstrap = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bootstrap)


def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def run(args):
    for proc in Path('/proc').iterdir():
        try:
            if (proc / 'exe').resolve().name in ('java', 'javac'):
                raise RuntimeError('All JVM work must be serial')
        except (FileNotFoundError, PermissionError):
            pass
    with socket.socket() as port:
        port.bind(('127.0.0.1', 25565))
    for key in ('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS'):
        if os.environ.get(key):
            raise ValueError('Inherited JVM options are prohibited: ' + key)
    source = args.install.resolve()
    argument_file = {'forge': 'net/minecraftforge/forge/1.21.1-52.1.0/unix_args.txt',
                     'neoforge': 'net/neoforged/neoforge/21.1.256/unix_args.txt'}[args.host]
    assert (source / 'libraries' / argument_file).is_file()
    for line in (source / 'user_jvm_args.txt').read_text().splitlines():
        if line.strip() and not line.lstrip().startswith('#'):
            raise ValueError('Expected official default user_jvm_args.txt')
    output = args.output.resolve()
    if not output.is_relative_to(REPO / 'out'):
        raise ValueError('Output must be below this worktree out/')
    output.mkdir(parents=True, exist_ok=False)
    server = output / 'server'
    server.mkdir()
    shutil.copytree(source / 'libraries', server / 'libraries')
    startup_files = ['run.sh', 'user_jvm_args.txt']
    if args.host == 'forge':
        startup_files.append('forge-1.21.1-52.1.0-shim.jar')
    for name in startup_files:
        shutil.copyfile(source / name, server / name)
    (server / 'eula.txt').write_text('eula=true\n')
    (server / 'mods').mkdir()
    for jar in args.mod:
        target = server / 'mods' / jar.name
        if target.exists():
            raise ValueError('Duplicate installed filename')
        shutil.copyfile(jar, target)
    paths = [*[server / name for name in startup_files],
             *sorted((server / 'libraries').rglob('*')), *sorted((server / 'mods').iterdir())]
    records = [{'path': str(p.relative_to(server)), 'sha256': sha(p), 'bytes': p.stat().st_size}
               for p in paths if p.is_file()]
    save(output / 'installed-inputs.json', records)
    env = os.environ.copy()
    env['PATH'] = str(args.jdk_home.resolve() / 'bin') + os.pathsep + env['PATH']
    expected = ['@user_jvm_args.txt', '@libraries/' + argument_file, 'nogui']
    save(output / 'command.json', {'command': ['sh', 'run.sh', 'nogui'], 'cwd': str(server),
         'expectedJavaArguments': expected, 'host': args.host, 'injectedJvmOptions': [],
         'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip(),
         'runnerSHA256': sha(Path(__file__)), 'console': args.console})
    state, seen_java = 'TIMEOUT', False
    started = time.monotonic()
    with (output / 'stdout.log').open('wb') as out, (output / 'stderr.log').open('wb') as err:
        process = subprocess.Popen(['sh', 'run.sh', 'nogui'], cwd=server, env=env,
                 stdin=subprocess.PIPE, stdout=out, stderr=err, start_new_session=True)
        try:
            while time.monotonic() - started < args.timeout:
                if not seen_java:
                    for proc in Path('/proc').iterdir():
                        try:
                            if (proc / 'exe').resolve().name == 'java' and (proc / 'cwd').resolve() == server:
                                actual = [v.decode() for v in (proc / 'cmdline').read_bytes().split(b'\0') if v]
                                if args.host == 'forge' and actual[1:] == ['-jar', startup_files[-1], '--onlyCheckJava']:
                                    save(output / 'official-java-check-command.json', actual)
                                    continue
                                save(output / 'actual-java-command.json', actual)
                                if actual[1:] != expected:
                                    raise ValueError('Non-stock actual JVM command')
                                seen_java = True
                        except (FileNotFoundError, PermissionError):
                            pass
                if 'Done (' in (output / 'stdout.log').read_text(errors='replace'):
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
                time.sleep(.5)
        finally:
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
    for name in ('logs', 'crash-reports'):
        if (server / name).exists():
            shutil.copytree(server / name, output / name)
    stdout = (output / 'stdout.log').read_text(errors='replace')
    runtime = '\n'.join(p.read_text(errors='replace') for p in (output / 'logs').glob('*.log'))
    checks = {'stockCommandCaptured': seen_java, 'DoneStopExit0': state == 'DONE_STOPPED'
              and process.returncode == 0 and 'Stopping server' in stdout,
              'stderrEmpty': (output / 'stderr.log').stat().st_size == 0,
              'noCrash': not any((output / 'crash-reports').glob('*')),
              'inputsUnchanged': all(sha(server / v['path']) == v['sha256'] for v in records)}
    save(output / 'result.json', {'lifecycle': 'PASS' if all(checks.values()) else 'FAIL',
         'modFunctionality': 'NOT ASSERTED', 'state': state, 'exitCode': process.returncode,
         'seconds': round(time.monotonic() - started, 2), 'assertions': checks,
         'severityRecords': bootstrap.severity_records(stdout + '\n' + runtime),
         'installedMods': [v for v in records if v['path'].startswith('mods/')]})
    # Only this run's verified physical dependency copies are discarded. Evidence/worlds remain.
    if not args.keep_runtime and checks['inputsUnchanged']:
        save(output / 'dependency-copy-cleanup.json', {'scope': str(server / 'libraries'),
             'matchedInstalledHashes': True, 'retained': ['world', 'logs', 'mods', 'results', 'commands']})
        shutil.rmtree(server / 'libraries')
    print(json.dumps(json.loads((output / 'result.json').read_text()), indent=2), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('install', 'output', 'jdk-home'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--host', choices=('forge', 'neoforge'), required=True)
    parser.add_argument('--mod', type=Path, action='append', default=[])
    parser.add_argument('--console', action='append', default=[])
    parser.add_argument('--timeout', type=int, default=240)
    parser.add_argument('--keep-runtime', action='store_true')
    run(parser.parse_args())
