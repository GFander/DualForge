"""Drive official console/tick/slot commands; never assign furnace burn/cook fields."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import sys
import time

ROOT = Path(__file__).resolve().parents[4]
spec = importlib.util.spec_from_file_location('stock', ROOT/'tests/real_mods/food_n_fuel/run_stock.py')
stock = importlib.util.module_from_spec(spec)
spec.loader.exec_module(stock)
PROPERTIES = 'level-seed=442526\n'
CASES = [
    ('paper', 'furnace', 'minecraft:cobblestone', 2, 'fnf:paper_stack'),
    ('bio', 'furnace', 'minecraft:cobblestone', 20, 'fnf:bio_fuel'),
    ('nonfuel', 'furnace', 'minecraft:cobblestone', 2, 'fnf:logo'),
    ('invalid-input', 'furnace', 'minecraft:diamond', 2, 'fnf:bio_fuel'),
    ('empty-input', 'furnace', None, 0, 'fnf:bio_fuel'),
    ('empty-fuel', 'furnace', 'minecraft:cobblestone', 2, None),
    ('wrong-recipe-type', 'smoker', 'minecraft:cobblestone', 2, 'fnf:bio_fuel'),
    ('vanilla-calibration', 'furnace', 'minecraft:cobblestone', 20, 'minecraft:coal'),
]


def driver(process, output):
    records = []
    plain = lambda: re.sub(r'\x1b\[[0-9;]*m', '', (output/'stdout.log').read_text(errors='replace'))

    def command(text):
        start = len(plain())
        marker = 'DF4C5_ACK_' + str(len(records))
        process.stdin.write((text+'\nsay '+marker+'\n').encode())
        process.stdin.flush()
        deadline = time.monotonic()+60
        while time.monotonic() < deadline:
            tail = plain()[start:]
            if '[Server] '+marker in tail:
                record = {'command': text, 'response': tail, 'ordinal': len(records)}
                records.append(record)
                (output/'console-transcript.json').write_text(json.dumps(records, indent=2)+'\n')
                return record
            if process.poll() is not None:
                raise ValueError('Server exited during console command: '+text)
            time.sleep(.05)
        raise ValueError('Console acknowledgement timeout: '+text)

    def game_time():
        response = command('time query gametime')['response']
        m = re.search(r'The time is (\d+)', response)
        if not m:
            raise ValueError('Missing official game-time response: '+response)
        return int(m[1])

    command('tick freeze')
    command('tick rate 1000')
    command('forceload add 0 0')
    for x, (name, block, item, count, fuel) in enumerate(CASES):
        command('setblock '+str(x)+' 100 0 minecraft:'+block)
        if item:
            command('item replace block '+str(x)+' 100 0 container.0 with '+item+' '+str(count))
        if fuel:
            command('item replace block '+str(x)+' 100 0 container.1 with '+fuel+' 1')
    # Campfires are not slotted fuel containers. This tests the actual unsupported
    # environment, not a claim that FnF rejects valid smoking/blasting fuel types.
    command('setblock 9 100 0 minecraft:campfire')
    command('item replace block 9 100 0 container.1 with fnf:bio_fuel 1')
    origin = game_time()
    snapshots = []
    previous = 0
    for elapsed in [0, 1, 21, 100, 101, 200, 201, 3200, 3201]:
        if elapsed:
            command('tick step '+str(elapsed-previous))
            deadline = time.monotonic()+60
            actual = game_time()
            while actual-origin < elapsed and time.monotonic() < deadline:
                time.sleep(.1)
                actual = game_time()
            if actual-origin != elapsed:
                raise ValueError('Official world ticks did not match requested step')
        else:
            actual = origin
        for x, case in enumerate(CASES):
            r = command('data get block '+str(x)+' 100 0')
            snapshots.append({'case':case[0], 'elapsed':elapsed, 'worldGameTime':actual, 'response':r['response']})
        previous = elapsed
    (output/'furnace-snapshots.json').write_text(json.dumps({'cases':CASES, 'snapshots':snapshots,
        'mechanism':'official server ticks with item-replace slots; no burn/cook/NBT assignment'},indent=2)+'\n')
    command('tick rate 20')
    command('tick unfreeze')
    command('save-all flush')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for name in ['install','jdk-home','output']:
        p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--host',choices=['forge','neoforge'],required=True)
    p.add_argument('--mod',type=Path,action='append',default=[])
    p.add_argument('--furnace',action='store_true')
    a=p.parse_args()
    for jar in a.mod:
        expected = {'fnf-forge-1.3-1.21.x.jar':'bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958',
                    'DualForge.jar':'a26893598d1ffdb73078373b007a4d5d54dddd1b774cb2a0fa0dd90fe5602bf7'}.get(jar.name)
        if expected and hashlib.sha256(jar.read_bytes()).hexdigest()!=expected:
            p.error('Frozen artifact SHA mismatch: '+jar.name)
    a.console=['forge mods' if a.host=='forge' else 'neoforge mods']
    a.timeout=600
    a.keep_runtime=False
    # Full process command/cwd, source SHA and configuration are preserved by the wrapper caller.
    return stock.run(a, console_driver=driver if a.furnace else None, initial_properties=PROPERTIES)


if __name__=='__main__':
    raise SystemExit(main())
