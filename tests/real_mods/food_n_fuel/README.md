# Food N' Fuel release evidence (007C)

This test uses the untouched official Forge 1.3 / MC 1.21.x release,
[CurseForge file 5842396](https://www.curseforge.com/minecraft/mc-mods/food-n-fuel/files/5842396).
Third-party binaries, installations, worlds and raw results stay under ignored `out/`.
Release bytecode is authoritative; public source is a reference, not an exact-source claim.

`run_stock.py` physically copies an official Forge 52.1.0 or NeoForge 21.1.256
installation and runs `sh run.sh nogui`. It records the actual JVM command,
inputs, console commands, logs and exit. Default JVM argument files are required;
inherited JVM options and simultaneous JVM work are refused. It deletes only its
own unchanged dependency copy after recording hashes; `--keep-runtime` retains it.
Its `lifecycle` result says nothing about third-party functionality or clean logs.
Severe log records remain visible and must be compared with the empty-host baseline.

```sh
python3 -B tests/real_mods/food_n_fuel/run_stock.py \
  --host forge --install /absolute/official-forge-install \
  --jdk-home /absolute/java21 --output "$PWD/out/convergence-007c/F0" \
  --console 'forge mods'
# F1: use a new output and add --mod /absolute/fnf-forge-1.3-1.21.x.jar
# N0/N1/N2: --host neoforge with the official NeoForge installation;
# N1 adds only DualForge.jar; N2 adds the same frozen Food N' Fuel JAR.
```

`ReleaseAbiDump.java` reads all release classes as ASM nodes, without loading or
rewriting them, and invokes the current candidate guard on the original JAR.
Compile/run with the tested DualForge JAR and its official NeoForge library classpath.
Save `javap -v -p -c` separately for all five original release classes.

`NativeRegistryObserver.java` is an optional **separate diagnostic run**, compiled
with official Forge libraries by `build_observer.py`. F0 remains empty and F1
contains only the official Food N' Fuel JAR. The diagnostic adds this third JAR
only to observe real RegisterEvent stacks, all existing registry items, holder
identity, food-property API results, custom fuel API results and the bound tab.
It never registers items or alters third-party behavior. API queries do not prove
food consumption, furnace burning, creative UI or player inventory/hand rendering.

```sh
python3 -B tests/real_mods/food_n_fuel/build_observer.py \
  --install /absolute/official-forge-install --jdk-home /absolute/java21 \
  --output "$PWD/out/convergence-007c/new-observer-build"
```

All JVMs run serially. Native player functionality is a prerequisite to production
expansion. A registry-only native baseline is PARTIAL; unavailable interactions
and graphics are NOT RUN. No third-party compatibility PASS follows from Done.
