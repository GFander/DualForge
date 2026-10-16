# FnF actual server behavior (007C-4C-5)

Use the unchanged official FnF release and accepted 4C-3 DualForge JAR. Never build
replacement inputs or change production. The planned matrix is [MATRIX](MATRIX.md).
All JVMs/compilers run serially in new directories under this worktree's out/.

```sh
python3 -B tests/real_mods/food_n_fuel/behavior/build_observer.py \
  --host forge --install "$FORGE_INSTALL" --jdk-home "$JDK" --output "$PWD/out/fresh-forge-observer"
python3 -B tests/real_mods/food_n_fuel/behavior/run_behavior.py \
  --host forge --install "$FORGE_INSTALL" --jdk-home "$JDK" \
  --output "$PWD/out/fresh-forge-fuel" --mod "$ORIGINAL_FNF" --furnace
# In a separate fresh run add --mod <fresh-forge-observer/forge-behavior-observer.jar>.
# Establish all native behavior first; only then compile/run the Neo observer.
python3 -B tests/real_mods/food_n_fuel/behavior/build_observer.py \
  --host neoforge --install "$NEO_INSTALL" --jdk-home "$JDK" --output "$PWD/out/fresh-neo-observer"
python3 -B tests/real_mods/food_n_fuel/behavior/run_behavior.py \
  --host neoforge --install "$NEO_INSTALL" --jdk-home "$JDK" \
  --output "$PWD/out/fresh-neo-control" --mod "$COMPAT" --mod "$NATIVE" --furnace
# Treatment: a new output, same options plus --mod "$ORIGINAL_FNF".
# Independent observed C/T: same additional Neo observer JAR in both new outputs.
python3 -B -m unittest discover -s tests/real_mods/food_n_fuel/behavior -p 'test_*.py' -v
```

Also run each empty host (no --mod or --furnace) for this round's severity baseline.
`run_behavior.py` uses stock `sh run.sh nogui`, fixed initial level-seed=442526,
verified physical library copies and no extra JVM/Agent/business flags. The shared
stock runner has two optional Python-only hooks (console driver/initial properties);
its ordinary CLI retains its existing behavior. Initial properties are recorded
separately because Minecraft expands/rewrites the generated server configuration.

The console driver places actual furnaces/smoker, fills only input/fuel slots via
`item replace`, and uses official frozen-world tick steps at tick rate 1000. It
checks real world game-time offsets before sampling full `data get block` and the
actual lit block state. No BurnTime/CookTime assignment or output-slot writes.
3200 means world ticks, not 3200 elapsed wall-clock ticks at the default rate.
All commands/acknowledgements and raw SNBT are retained. `snbt.py` fails on missing,
ambiguous or incomplete snapshot state; parser tests never run a game.

Campfire input refusal is precisely `Target position 9, 100, 0 is not a container`
when FnF is installed. In Control, the earlier official parser refusal is exactly
`Unknown item 'fnf:bio_fuel'`. Do not confuse this intended installation difference
with a Forge/Neo behavior difference. Cobble in a smoker has no valid smoking
recipe; it does not establish that FnF rejects valid smoker fuels.

The native observer only queries Registry/Holder identity, all actual FOOD
components and the public late CreativeModeTab API. Private cached icon state is
read, never written; private FnF lambdas are never invoked directly or wrapped.
Null icon/empty display before the official query plus the pinned host bytecode
bound the lazy-execution proof; there is no global uninstrumented callback counter.
Food conversion components are queried; no container return or player consumption
is simulated. Gate failures preserve their Throwable independently of host boot.

Raw commands/results, all eight runs, first failed checker and corrected exact
assessment stay in the task out/ tree. Standard and observer cohorts are separate;
all C/T shared files, observer SHA, initial world properties and JVM commands must
match, with only the original FnF JAR added. CI remains static/unit; the new parser
suite and live behavior are run explicitly. No historical game matrix is implied.

Client creative UI, real player consumption and restart/item persistence are
separate NOT RUN gates. New evidence ZIP needs its own publication authorization.
