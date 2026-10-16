# Original Food N' Fuel server experiment (007C-4C-4)

This experiment runs the unchanged official Forge release after merged PR #28.
Production Java/resources are frozen. A runtime failure is an experimental result,
not permission to repair compatibility in this task.

`run_stage.py` checks the exact FnF and 4C-3 compat digests, delegates to the
existing stock runner, and saves invocation/runner exit/installed hashes. It never
declares Item registration or functionality from a successful lifecycle alone.
Use fresh output directories; all JVMs and compilers run serially.

```sh
python3 -B tests/real_mods/food_n_fuel/runtime/run_stage.py \
  --host forge --install "$FORGE_INSTALL" --jdk-home "$JDK" \
  --output "$PWD/out/fresh/forge-fnf" --fnf "$ORIGINAL_FNF"
python3 -B tests/real_mods/food_n_fuel/runtime/run_stage.py \
  --host neoforge --install "$NEO_INSTALL" --jdk-home "$JDK" \
  --output "$PWD/out/fresh/control" --compat "$COMPAT" --native "$NATIVE"
python3 -B tests/real_mods/food_n_fuel/runtime/run_stage.py \
  --host neoforge --install "$NEO_INSTALL" --jdk-home "$JDK" \
  --output "$PWD/out/fresh/treatment" --compat "$COMPAT" --native "$NATIVE" \
  --fnf "$ORIGINAL_FNF"
```

First run each host with no mods to establish this round's actual error baseline.
For a separate read-only native Forge registry query, compile the unchanged
`../NativeRegistryObserver.java` via `../build_observer.py`, then add its JAR with
`--observer` to a new Forge FnF run. Keep this extra observer run distinct from the
standard experiment. Registry/Holder/Tab observations are neither player food/fuel
behavior nor client UI or persistence verification.

Control and Treatment use the same official NeoForge input, final 4C-3 compat,
native coexistence JAR and stock launcher arguments; only FnF is added. No Agent,
business environment flag, extra module parameter or diagnostic SPI is installed.
Source ClassFiles/javap and existing offline planned bytes are not evidence that
the JVM defined or executed a class. Actual transformation logs, exception frames,
bridge receipts and real registry queries establish those separate boundaries.

Outputs include stock commands, captured real Java commands, stdout/stderr,
exit status, installed-input digests, debug logs and crash reports. Physical
copies isolate new worlds; only each run's hash-verified dependency copies are
removed by the existing runner. Original installations, worlds and historical
results are not changed. Public evidence upload requires separate authorization.

For the separate NeoForge read-only observation pair, build
`runtime/build_observer.py --install "$NEO_INSTALL" --jdk-home "$JDK" --output <fresh>`.
Repeat C/T in new directories with the **same** additional `--observer` JAR.
The observer records actual native RegisterEvent stacks and queries the registries
at ServerStarted. It checks all 67 Item Holder identities, Tab Holder identity,
actual container Mod Bus and native Global Bus identity, zero native FnF listener
objects, and source module/layer. It never invokes icon/display callbacks, creates
items, replaces registration or intercepts production calls. This pair is distinct
from the standard C/T without the observer.
