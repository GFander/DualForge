"""Run an isolated stock host on SSD; record lifecycle separately from orb behavior."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import signal
import socket
import subprocess
import time

ROOT = Path(__file__).resolve().parents[3]
RUNTIME = Path.home() / 'Env/DualForge'
SPEC = importlib.util.spec_from_file_location(
    'snbt', ROOT / 'tests/real_mods/food_n_fuel/behavior/snbt.py')
SNBT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SNBT)


def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


class Console:
    def __init__(self, process, output):
        self.process, self.output, self.records = process, output, []

    def command(self, command):
        log = self.output / 'stdout.log'
        start = log.stat().st_size
        marker = 'DF008A_ACK_' + str(len(self.records))
        self.process.stdin.write((command + '\nsay ' + marker + '\n').encode())
        self.process.stdin.flush()
        deadline = time.monotonic() + 60
        while time.monotonic() < deadline:
            with log.open('rb') as stream:
                stream.seek(start)
                tail = stream.read().decode(errors='replace')
            tail = re.sub(r'\x1b\[[0-9;]*m', '', tail)
            if '[Server] ' + marker in tail:
                self.records.append({'command': command, 'response': tail})
                save(self.output / 'console-transcript.json', self.records)
                return tail
            if self.process.poll() is not None:
                raise ValueError('Server exited during command: ' + command)
            time.sleep(.05)
        raise ValueError('Console acknowledgement timeout: ' + command)

    def game_time(self):
        response = self.command('time query gametime')
        match = re.search(r'The time is (\d+)', response)
        if not match:
            raise ValueError('Missing world game-time response: ' + response)
        return int(match[1])

    def snapshot(self):
        response = self.command('execute as @e[type=minecraft:experience_orb,tag=df008a] run data get entity @s')
        records = [SNBT.parse(line) for line in re.findall(
            r'has the following entity data: (\{[^\n]+)', response)]
        if not records:
            raise ValueError('No actual experience-orb entity data')
        for entity in records:
            if not {'UUID', 'Value', 'Count', 'Pos'}.issubset(entity):
                raise ValueError('Incomplete experience-orb snapshot')
        total = sum(sum(int(k) * v for k, v in e['clumpedMap'].items())
                    if 'clumpedMap' in e else e['Value'] * e['Count'] for e in records)
        result = {'gameTime': self.game_time(), 'entities': records,
                  'entityCount': len(records), 'totalXP': total}
        return result

    def orbs(self):
        self.command('tick freeze')
        self.command('tick rate 1000')
        self.command('forceload add 0 0')
        deadline = time.monotonic() + 60
        while '[Server] DF008A_CHUNK_READY' not in self.command(
                'execute if loaded 0 100 0 run say DF008A_CHUNK_READY'):
            if time.monotonic() > deadline:
                raise ValueError('Test chunk did not load')
            time.sleep(.1)
        self.command('fill 0 99 0 4 99 4 minecraft:stone')
        # Initial summon data creates ordinary real orbs. Never write Count,
        # clumpedMap, age, merge results, or existing orb NBT.
        for value in [3, 7, 11]:
            self.command('summon minecraft:experience_orb 2 100 2 '
                         '{Value:' + str(value) + 's,Tags:["df008a"]}')
        before = self.snapshot()
        if before['entityCount'] != 3 or before['totalXP'] != 21:
            raise ValueError('Invalid initial real-orb cohort')
        self.command('tick step 400')
        deadline = time.monotonic() + 60
        actual = self.game_time()
        while actual - before['gameTime'] < 400 and time.monotonic() < deadline:
            time.sleep(.1)
            actual = self.game_time()
        if actual - before['gameTime'] != 400:
            raise ValueError('Requested world ticks did not complete exactly')
        after = self.snapshot()
        save(self.output / 'orb-snapshots.json', {'before': before, 'after': after,
             'totalPreserved': before['totalXP'] == after['totalXP'],
             'merged': after['entityCount'] < before['entityCount'],
             'mechanism': 'summon real orbs with distinct initial values; official 400 world ticks; read-only NBT'})
        self.command('save-all flush')

    def recovered_orbs(self, previous):
        self.command('tick freeze')
        self.command('forceload add 0 0')
        deadline = time.monotonic() + 60
        while '[Server] DF008A_CHUNK_READY' not in self.command(
                'execute if loaded 0 100 0 run say DF008A_CHUNK_READY'):
            if time.monotonic() > deadline:
                raise ValueError('Reloaded test chunk did not load')
            time.sleep(.1)
        actual = self.snapshot()
        normalize = lambda state: sorted(
            [(e['UUID'], e['Value'], e['Count'], e.get('clumpedMap')) for e in state['entities']])
        result = {'expected': previous, 'actual': actual,
                  'sameUUIDValueCountMap': normalize(previous) == normalize(actual),
                  'totalPreserved': previous['totalXP'] == actual['totalXP']}
        save(self.output / 'orb-reload.json', result)
        if not result['sameUUIDValueCountMap'] or not result['totalPreserved']:
            raise ValueError('Persisted orb identity/value/map did not recover')
        self.command('save-all flush')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host', choices=['forge', 'neoforge'], required=True)
    parser.add_argument('--install', type=Path, required=True)
    parser.add_argument('--jdk-home', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--mod', type=Path, action='append', default=[])
    behavior = parser.add_mutually_exclusive_group()
    behavior.add_argument('--orbs', action='store_true')
    behavior.add_argument('--reload-from', type=Path)
    behavior.add_argument('--fnf-furnace', action='store_true')
    args = parser.parse_args()
    output, install = args.output.resolve(), args.install.resolve()
    if not output.is_relative_to((RUNTIME / 'runs').resolve()):
        parser.error('Runtime output must be below ~/Env/DualForge/runs on SSD')
    for key in ['JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS']:
        if os.environ.get(key):
            parser.error('Inherited JVM options prohibited: ' + key)
    for proc in Path('/proc').iterdir():
        try:
            if (proc / 'exe').resolve().name in ['java', 'javac']:
                parser.error('All JVM experiments must be serial')
        except (FileNotFoundError, PermissionError):
            pass
    with socket.socket() as port:
        port.bind(('127.0.0.1', 25565))
    if any(line.strip() and not line.lstrip().startswith('#')
           for line in (install / 'user_jvm_args.txt').read_text().splitlines()):
        parser.error('Expected official default JVM argument file')
    output.mkdir(parents=True, exist_ok=False)
    save(output / 'invocation.json', {key: ([str(p) for p in value] if isinstance(value, list)
         else str(value) if isinstance(value, Path) else value) for key, value in vars(args).items()})
    server = output / 'server'
    server.mkdir()
    shutil.copytree(install / 'libraries', server / 'libraries')
    startup = ['run.sh', 'user_jvm_args.txt']
    if args.host == 'forge':
        startup.append('forge-1.21.1-52.1.0-shim.jar')
    for name in startup:
        shutil.copyfile(install / name, server / name)
    (server / 'mods').mkdir()
    for jar in args.mod:
        target = server / 'mods' / jar.name
        if target.exists():
            raise ValueError('Duplicate installed mod filename')
        shutil.copyfile(jar, target)
    previous_orbs = None
    if args.reload_from:
        previous = args.reload_from.resolve()
        if previous.parent != output.parent or previous == output:
            raise ValueError('Reload source must be a different experiment in the same task directory')
        old_inputs = json.loads((previous / 'installed-inputs.json').read_text())
        expected_mods = {r['path']: r['sha256'] for r in old_inputs if r['path'].startswith('mods/')}
        actual_mods = {str(p.relative_to(server)): sha(p) for p in (server / 'mods').iterdir()}
        if expected_mods != actual_mods or json.loads((previous / 'result.json').read_text())['lifecycle'] != 'PASS':
            raise ValueError('Reload requires matching immutable mods and a successful source experiment')
        previous_orbs = json.loads((previous / 'orb-snapshots.json').read_text())['after']
        source_world = previous / 'server/world'
        world_records = {str(p.relative_to(source_world)): sha(p) for p in source_world.rglob('*') if p.is_file()}
        shutil.copytree(source_world, server / 'world')
        if any(sha(server / 'world' / name) != digest or sha(source_world / name) != digest
               for name, digest in world_records.items()):
            raise ValueError('World physical copy mismatch')
        save(output / 'world-copy.json', {'source': str(source_world), 'files': world_records,
             'scope': 'fresh experiment world only; copied before restart, never modify source'})
    properties = 'server-ip=127.0.0.1\nlevel-seed=442526\n'
    (server / 'eula.txt').write_text('eula=true\n')
    (server / 'server.properties').write_text(properties)
    (output / 'initial-server.properties').write_text(properties)
    inputs = [*sorted((server / 'libraries').rglob('*')),
              *sorted((server / 'mods').iterdir()), *[server / n for n in startup]]
    records = [{'path': str(p.relative_to(server)), 'bytes': p.stat().st_size,
                'sha256': sha(p)} for p in inputs if p.is_file()]
    save(output / 'installed-inputs.json', records)
    save(output / 'source-inputs.json', [
        {'path': str(p.resolve()), 'bytes': p.stat().st_size, 'sha256': sha(p)}
        for p in [Path(__file__), *args.mod]])
    host_args = ('net/minecraftforge/forge/1.21.1-52.1.0/unix_args.txt' if args.host == 'forge'
                 else 'net/neoforged/neoforge/21.1.256/unix_args.txt')
    expected = ['@user_jvm_args.txt', '@libraries/' + host_args, 'nogui']
    command = ['sh', 'run.sh', 'nogui']
    save(output / 'command.json', {'argv': command, 'cwd': str(server), 'expectedJavaArguments': expected,
         'jdkHome': str(args.jdk_home.resolve()), 'injectedJvmOptions': [],
         'fixtureEnvironment': {key: os.environ[key] for key in ['DF4C3_VARIANT', 'DF4C3_REGISTER', 'DF4B_REGISTER'] if key in os.environ},
         'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()})
    env = os.environ.copy()
    env['PATH'] = str(args.jdk_home.resolve() / 'bin') + os.pathsep + env['PATH']
    seen, state, failure = False, 'TIMEOUT', None
    started = time.monotonic()
    with (output / 'stdout.log').open('wb') as out, (output / 'stderr.log').open('wb') as err:
        process = subprocess.Popen(command, cwd=server, env=env, stdin=subprocess.PIPE,
                                   stdout=out, stderr=err, start_new_session=True)
        try:
            while time.monotonic() - started < 600:
                if not seen:
                    for proc in Path('/proc').iterdir():
                        try:
                            if (proc / 'exe').resolve().name != 'java' or (proc / 'cwd').resolve() != server:
                                continue
                            actual = [v.decode() for v in (proc / 'cmdline').read_bytes().split(b'\0') if v]
                            if args.host == 'forge' and actual[1:] == ['-jar', startup[-1], '--onlyCheckJava']:
                                save(output / 'official-java-check-command.json', actual)
                                continue
                            save(output / 'actual-java-command.json', actual)
                            if actual[1:] != expected:
                                raise ValueError('Non-stock actual JVM command')
                            seen = True
                        except (FileNotFoundError, PermissionError):
                            pass
                if 'Done (' in (output / 'stdout.log').read_text(errors='replace'):
                    console = Console(process, output)
                    console.command('forge mods' if args.host == 'forge' else 'neoforge mods')
                    if args.orbs:
                        console.orbs()
                    elif previous_orbs is not None:
                        console.recovered_orbs(previous_orbs)
                    elif args.fnf_furnace:
                        spec = importlib.util.spec_from_file_location(
                            'fnf_driver', ROOT / 'tests/real_mods/food_n_fuel/behavior/run_behavior.py')
                        driver = importlib.util.module_from_spec(spec)
                        spec.loader.exec_module(driver)
                        driver.driver(process, output)
                    process.stdin.write(b'stop\n')
                    process.stdin.flush()
                    process.wait(timeout=90)
                    state = 'DONE_STOPPED'
                    break
                if process.poll() is not None:
                    state = 'EXITED_BEFORE_DONE'
                    break
                time.sleep(.1)
        except Exception as error:
            state, failure = 'HARNESS_FAILURE', {'type': type(error).__name__, 'message': str(error)}
        finally:
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
    unchanged = all(sha(server / r['path']) == r['sha256'] for r in records)
    stdout = (output / 'stdout.log').read_text(errors='replace')
    checks = {'stockCommandCaptured': seen, 'doneStopExit0': state == 'DONE_STOPPED' and process.returncode == 0,
              'inputsUnchanged': unchanged, 'noCrash': not any((server / 'crash-reports').glob('*')),
              'stderrEmpty': (output / 'stderr.log').stat().st_size == 0}
    if args.reload_from:
        checks['reloadSourceWorldUnchanged'] = all(sha(source_world / name) == digest
                                                   for name, digest in world_records.items())
    result = {'lifecycle': 'PASS' if all(checks.values()) else 'FAIL', 'state': state,
              'exitCode': process.returncode, 'checks': checks, 'failure': failure,
              'seconds': round(time.monotonic() - started, 2),
              'severityLines': [line for line in stdout.splitlines() if re.search(r'[/ ](?:ERROR|FATAL)[\] ]', line)],
              'functionality': ('see orb-snapshots.json' if args.orbs else 'see orb-reload.json'
                               if args.reload_from else 'see furnace-snapshots.json' if args.fnf_furnace else 'NOT_RUN')}
    save(output / 'result.json', result)
    print(json.dumps(result, indent=2), flush=True)
    return 0 if all(checks.values()) else 1


if __name__ == '__main__':
    raise SystemExit(main())
