# Controlled Registry Helper (007C-4B)

The source fixture is compiled with official Forge 52.1.0 only. A no-argument
entry obtains its Forge Context bus and calls an independent Item helper. The
helper declares two ordinary Items using a zero-capture lambda and a static
method reference. A separate Consumer checks Holder fields, method returns and
`get()` identity. `Unused` is outside the actual reference closure.

`fixture-identity.json` records the first, unchanged Forge JAR and the separately
compiled platform observers. Its absolute paths identify external evidence;
they are not downloadable dependencies. Rebuilding on another machine produces
a new cohort and requires a new native Forge baseline before NeoForge testing.

From the repository root, with external official installations and Java 21:

```zsh
python3 -B tests/helper_registry/build_fixture.py \
  --forge-install "$FORGE_INSTALL" --neo-install "$NEO_INSTALL" --jdk-home "$JDK"
python3 -B tests/helper_registry/run_contract.py \
  --jar "$COMPAT" --fixture "$FIXTURE" --server-input "$NEO_INSTALL" \
  --jdk-home "$JDK" --fnf "$ORIGINAL_FNF"
python3 -B tests/helper_registry/run_runtime.py \
  --host forge --stage treatment --fixture-index "$FIXTURE_INDEX" \
  --install "$FORGE_INSTALL" --jdk-home "$JDK" \
  --baseline "$FORGE_EMPTY_RESULT" --output "$PWD/out/helper-native"
```

First capture empty-host baselines using
`tests/real_mods/food_n_fuel/run_stock.py --host forge/neoforge`, with `--install`,
`--jdk-home` and a fresh `--output`. Run the NeoForge command twice, changing
`--stage control` to `--stage treatment` and choosing distinct output paths:

```zsh
python3 -B tests/helper_registry/run_runtime.py \
  --host neoforge --stage control --fixture-index "$FIXTURE_INDEX" \
  --install "$NEO_INSTALL" --jdk-home "$JDK" --jar "$DUALFORGE_DELIVERY" \
  --native "$NATIVE_CONTROL" --baseline "$NEO_EMPTY_RESULT" \
  --output "$PWD/out/helper-control"
```

`$DUALFORGE_DELIVERY` must be named `DualForge.jar`; its bytes must equal the
contract's `dualforge.jar`. C/T use the same fixture, observer and installed
JAR set; only fixture environment `DF4B_REGISTER=false/true` changes. The runner
uses official stock launch files, checks installed SHA, real registries,
Supplier counts/official event stack, independent container Bus identity,
consumer identity, native coexistence, lifecycle and same-run empty-host errors.
`cohort.validate()` rejects mixed Contract/C/T SHA and conditions; Python unit
tests cover mismatched builds. A functional PASS is not a zero-ERROR claim.

`run_bridge_contract.py` takes `--jar`, `--fixture-index`, `--peer` (the frozen
C3 contextpeer), `--install`, `--jdk-home`, `--baseline` and fresh `--output`.
It is a separate diagnostic launch using `DF4B_BRIDGE_NEGATIVES=true`. White-box
negative inputs use real containers, private view construction and restored
thread-local active-state changes. They are not part of standard C/T and do not
prove a security sandbox. No production raw-bus accessor is added.

`run_ci_contract.py --java-home "$JDK"` downloads three SHA-pinned ASM jars and
tests the actual production stack verifier without a game installation. Optional
`--libraries "$NEO_INSTALL/libraries"` reuses matching official jars. This is a
synthetic bytecode contract, not native Forge or game evidence.

All JVM work is serial and uses physical runtime copies. Reports and exact
commands live in `out/convergence-007c4b/`; logs/worlds/libraries stay out of Git.
Gate 8 FnF admission is read-only and currently REJECTED. The platform matrix's
explicit `--skip-fnf-runtime` excludes N2 under that gate; the other 18 cases
keep every original assertion. The default historical matrix is unchanged.
