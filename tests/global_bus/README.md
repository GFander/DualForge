# Controlled Global Event Bus / composed Registry plan (007C-4C-3)

Only a verified Context entry's exact `MinecraftForge.EVENT_BUS` → `register(this)`
constructor sequence is supported. The same immutable plan owns entry, typed Item/
Tab helpers and Holder consumers. No targets are published until source closure,
Global semantics and every cloned class pass validation. No generic Bus alias,
source post API, raw Bus accessor, arbitrary event or Registry support is added.

The old Context/Item/Block/custom-Event validators remain unchanged. This profile
accepts zero handlers or one public, non-static, non-inherited, default-options
ServerStartedEvent handler with the controlled marker body. Its business bytecode
is preserved. Unknown annotation/priority/cancellation/Bootstrap/control flow and
wrong source/object/timing are rejected.

Forge6.2.27 accepts an empty listener instance; Neo8.0.5 rejects it. The bridge
actually calls the official Global Bus with the original entry object. Only the
exact official, SHA-pinned zero-handler rejection is translated to Forge's empty
registration outcome. Other exceptions and a no-op native return are failures;
UNCERTAIN prevents retries. No artificial listener is registered for empty input.

Run from the repository root; configure absolute `JDK`, `FORGE_INSTALL`,
`NEO_INSTALL`, and fixed `PINNED_HOST_ORACLE_TSV` first. All JVM work is serial.

```zsh
python3 -B tests/global_bus/build_semantics.py \
  --forge-install "$FORGE_INSTALL" --neo-install "$NEO_INSTALL" \
  --jdk-home "$JDK" --output "$PWD/out/fresh-semantics-input"
python3 -B tests/global_bus/build_fixture.py \
  --forge-install "$FORGE_INSTALL" --neo-install "$NEO_INSTALL" \
  --jdk-home "$JDK" --output "$PWD/out/fresh-global-input"
```

The first builder creates separate native platform probes. Run the Forge probe
first using `tests/real_mods/food_n_fuel/run_stock.py`, then the NeoForge probe,
without compat. They record real empty/direct/duplicate/static/inherited/invalid
registrations and official ServerStartedEvent delivery. Invalid-input diagnostic
errors are distinct from the healthy empty-host baseline.

The second builder produces three original Forge52.1.0 JARs: quiet, signal and
combined. Platform observers compile separately. `fixture-identity.json` freezes
the actual originals and standard observers; rebuilding is a new cohort. Observer
fixes can use `--existing-fixture-index <old result.json>` to preserve all three
original Forge JAR bytes while rebuilding only platform observers. Retest changed
observers; do not replace historical results.

```zsh
python3 -B tests/global_bus/run_contract.py \
  --jar "$COMPAT" --fixture "$COMBINED" --quiet "$QUIET" --signal "$SIGNAL" \
  --server-input "$NEO_INSTALL" --jdk-home "$JDK" \
  --host-oracle "$PINNED_HOST_ORACLE_TSV" --output "$PWD/out/fresh-global-contract"
python3 -B tests/global_bus/run_runtime.py \
  --host forge --variant combined --stage treatment --fixture-index "$INDEX" \
  --install "$FORGE_INSTALL" --jdk-home "$JDK" --baseline "$FORGE_EMPTY_RESULT" \
  --output "$PWD/out/fresh-global-native"
python3 -B tests/global_bus/run_runtime.py \
  --host neoforge --variant combined --stage treatment --fixture-index "$INDEX" \
  --install "$NEO_INSTALL" --jdk-home "$JDK" --baseline "$NEO_EMPTY_RESULT" \
  --jar "$COMPAT" --native "$NATIVE_CONTROL" --output "$PWD/out/fresh-global-neo"
```

Run quiet and signal separately too. Use fresh output paths. Compat delivery is
named `DualForge.jar`. The combined C/T changes only `DF4C3_REGISTER` (Registry
attachment), retaining the same Global listener; `DF4C3_VARIANT=combined` is fixed.
`global_cohort.validate` checks the exact environment and all artifact identities.
Raw stock commands and actually installed hashes must match. Healthy severity
records compare against a same-task empty host; PASS does not imply zero ERROR.

Observers read the official Bus's private listener table solely for independent
object/count evidence (the actual Bus module is open). Empty input has zero native
source objects; listener input has exactly one whose identity must match the bridge
argument. Observers do not implement or replace production registration. Native
RegisterEvent, Item/Tab/Holder identity, deferred callbacks and normal stop are
verified; server tab queries do not establish client UI rendering.

`build_diagnostic.py --neo-install ... --jdk-home ... --fixture-index ... --output
<fresh>` creates a separate Neo observer. It tests a real constructor duplicate,
wrong public caller, and explicitly isolated private state-machine guards. A Bus
proxy first delegates a real native registration then throws: one real listener
remains and dispatches once, state stays UNCERTAIN and retry cannot call again.
Wrong empty exceptions and no-op returns fail. These white-box cases bypass some
caller admission checks; they do not establish a security sandbox or rollback.
Never mix this observer into standard C/T.

```zsh
python3 -B tests/global_bus/run_fnf.py --jar "$COMPAT" --fnf "$ORIGINAL_FNF" \
  --server-input "$NEO_INSTALL" --jdk-home "$JDK" \
  --host-oracle "$PINNED_HOST_ORACLE_TSV" --output "$PWD/out/fresh-fnf-plan"
```

`global-v1` requires exact complete-plan receipts and three unique per-target SHA
proofs. Every source target is cloned, checked against its raw SHA and ASM-verified.
No FnF class is defined/initialized and no game is launched: FNF_RUNTIME=NOT RUN.
Old legacy-4b/supplier-v1/typed-v1 receipts remain historical. The active old Context
regression now directly tests its unchanged validator; production FnF composition
is covered here. The old typed blanket-Global rejection is replaced with an
unsupported-post negative under this explicit new authorization. No other old
strict assertion is relaxed. Related 4B/C1/C2 dynamic and Bridge regressions must
use the same final new JAR. Public raw evidence upload needs separate permission.
