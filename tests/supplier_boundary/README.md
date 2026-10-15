# Controlled Item Supplier boundary (007C-4C-1)

The production verifier checks types, exact method/field linkage and forward
business dependencies. It leaves business instructions unchanged and keeps the
existing Registry API conversion. This is a compatibility contract, not a Java
security sandbox. No FnF identity selects a production rule.

`build_fixture.py` generates three independently named native Forge fixtures:
ordinary Item, independent FoodProperties data/factory, and an Item subclass
with an extra constructor parameter and a real override. Generated source,
class/JAR SHA, commands and pure Forge compilation inputs are saved in ignored
`out/convergence-007c4c1/fixture/`. Observers are compiled separately for each
platform. Rebuilding creates a new cohort that needs its own native baseline.

```zsh
python3 -B tests/supplier_boundary/build_fixture.py \
  --forge-install "$FORGE_INSTALL" --neo-install "$NEO_INSTALL" --jdk-home "$JDK"
python3 -B tests/supplier_boundary/run_contract.py \
  --jar "$COMPAT" --fnf "$ORIGINAL_FNF" --server-input "$NEO_INSTALL" \
  --jdk-home "$JDK" --host-oracle "$PINNED_HOST_ORACLE_TSV" \
  --fixture-index "$ORDINARY_INDEX" --fixture-index "$DATA_INDEX" \
  --fixture-index "$SUBCLASS_INDEX" --output "$PWD/out/supplier-contract"
```

`PinnedHostOracle` is an offline test dependency reading actual selected host
class providers recorded by 4C-A. It is never installed into game runs. Production
reads host ABI resources from the real FML Minecraft/NeoForge ModFiles and JDK.
The Contract saves pre/post ClassNodes, normalized business methods and exact
rejections. Individual verifier negatives have no publication API; full-plan
late-Supplier negatives separately prove failure returns no plan. Original 4B
Transformer contracts still check whole-group publication and clone atomicity.

Capture fresh empty-host results with `tests/real_mods/food_n_fuel/run_stock.py`.
For each fixture, run native Forge Treatment first, then NeoForge Control and
Treatment with the **same original fixture bytes**:

```zsh
python3 -B tests/supplier_boundary/run_runtime.py \
  --host forge --stage treatment --fixture-index "$CASE_INDEX" \
  --install "$FORGE_INSTALL" --jdk-home "$JDK" \
  --baseline "$FORGE_EMPTY_RESULT" --output "$PWD/out/case-native"
python3 -B tests/supplier_boundary/run_runtime.py \
  --host neoforge --stage control --fixture-index "$CASE_INDEX" \
  --install "$NEO_INSTALL" --jdk-home "$JDK" --jar "$DUALFORGE_DELIVERY" \
  --native "$NATIVE_CONTROL" --baseline "$NEO_EMPTY_RESULT" \
  --output "$PWD/out/case-control"
```

Repeat the second command with `--stage treatment` and a fresh output. Delivery
must be named `DualForge.jar`. Only `DF4C1_REGISTER=false/true` changes between
C/T; compatibility flags, extra modules and agents are not injected. All JVMs
run serially on physical copies. The observer checks real Registry/Holder/object
identity, exact Supplier/init counts, real Mod Bus, official RegisterEvent stack,
business values, native coexistence and normal stop. Food consumption, furnace
behavior and client rendering are separate NOT RUN gates.

Use `cohort.validate(contract, control, treatment,
register_environment="DF4C1_REGISTER")` from `tests/helper_registry/cohort.py`
to reject mixed compat/fixture/observer/installed hashes and conditions. A PASS
compares errors with the same-round empty host; it does not claim zero ERROR.
FnF is read-only with the versioned `supplier-v1` Contract in helper_registry;
normal candidacy still excludes its global Event Bus. Full FnF runtime is NOT RUN.
