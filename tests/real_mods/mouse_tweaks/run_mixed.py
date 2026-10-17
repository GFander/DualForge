"""Use the unchanged stock harness with FnF furnace commands followed by real Clumps orbs."""
import importlib.util
import sys
from pathlib import Path
from support import ROOT, record, save


def main():
    spec = importlib.util.spec_from_file_location('clumps_stock', ROOT / 'tests/real_mods/clumps/run_server.py')
    stock = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(stock)
    original = stock.Console.orbs
    furnace_path = ROOT / 'tests/real_mods/food_n_fuel/behavior/run_behavior.py'

    def combined(console):
        driver_spec = importlib.util.spec_from_file_location('fnf_measurement', furnace_path)
        driver = importlib.util.module_from_spec(driver_spec)
        driver_spec.loader.exec_module(driver)
        save(console.output / 'mixed-driver.json', {'scope': 'FnF official furnace commands, then real-orb commands; no injected game code/JVM options',
             'inputs': [record(Path(__file__)), record(ROOT / 'tests/real_mods/clumps/run_server.py'), record(furnace_path)]})
        driver.driver(console.process, console.output)
        original(console)

    stock.Console.orbs = combined
    return stock.main()


if __name__ == '__main__':
    sys.exit(main())
