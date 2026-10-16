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

All JVMs run serially. The 007C native player-functionality gate remains PARTIAL;
unavailable interactions and graphics are NOT RUN. The later 007C-1/007C-2 tasks
explicitly authorize admission/platform-dependency work while retaining that
native limitation. No third-party compatibility PASS follows from Done.

`ReleaseBoundaryTest.java` records the historical 007C NightConfig/guard boundary
on the actual release, then checks the old guard/Reader decline, unsupported constructor
profile and original ClassNode/JAR preservation. A passing boundary test does not
mean discovery was repaired. Contract JVM module options are diagnostic only.

The runner exits nonzero on failed lifecycle assertions even when the host JVM
returns zero. The final runner was exercised with a real FML missing-required-mod
negative. Its verified `zeroDualForgeParameters` field supports the existing stock
bootstrap baseline reader; older raw results keep their original schema.

That old decline Contract is not a current admission expectation after 007C-1;
the [metadata Contract](metadata/README.md) retains the original parsing/profile
checks and adds positive same-JAR ownership. The 007C-2 read-only ASM dump checks
all release nodes against the original 007C dump without rewriting them.

The frozen 007C result is L0 / PARTIAL with production unchanged at that time.
Current 007C-2 production projects only independently verified platform requirements;
original Food N' Fuel then fails in transformer targets on the existing Item class
structure check, before construction. See [platform contracts](dependency/README.md)
and [current delivery](<../../../Dev Manuals/Convergence-007C-2 交付报告.md>).
Historical [delivery](<../../../Dev Manuals/Convergence-007C 真实物品兼容交付报告.md>),
[index](<../../../Dev Manuals/Convergence-007C 证据索引.json>) and
[native client checklist](NATIVE-CLIENT-CHECKLIST.md).

## 007C-4C-0 read-only closure analysis

The [gap report](../../../Dev%20Manuals/Convergence-007C-4C-0%20FnF兼容缺口报告.md) and [six derived datasets](../../../Dev%20Manuals/Convergence-007C-4C-0/) distinguish current profile exclusion from exact forced-plan refusal. Use [fnf_gap.py](../../../tools/api-atlas/fnf_gap.py) after a new Atlas usage scan. All commands are offline/read-only; no FnF runtime is started, production support is unchanged, and C4A/client limitations remain explicit.

## 007C-4C-4 original release runtime experiment

The [runtime experiment](runtime/README.md) tests the original release on native Forge first, then stock NeoForge Control/Treatment with the exact accepted 4C-3 JAR. Production compatibility is frozen. Earlier NOT RUN/refusal receipts remain historical; actual runtime, registry observations and player/client behavior are reported separately.
