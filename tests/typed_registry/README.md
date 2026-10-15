# Controlled Typed Registry / Creative Tab (007C-4C-2)

Production supports two distinct Registry kinds: the existing Item path and
CreativeModeTab via `DeferredRegister.create(ResourceKey,String)`. A helper has
one typed register. IDs are compared as Registry key + namespace + path, so an
Item and Tab may legally share a path. This is not arbitrary Registry support.

The original native Forge fixture declares two Items and one Tab, a zero-capture
icon and display callback, and a separate Holder Consumer. No NeoForge or
DualForge API is a compile dependency of that input. Platform observers compile
separately. `fixture-identity.json` freezes the actual input and observers used
for the native and NeoForge runs; external paths identify evidence, not public
download dependencies. Rebuilding creates a new cohort requiring native proof.

```zsh
python3 -B tests/typed_registry/build_fixture.py \
  --forge-install "$FORGE_INSTALL" --neo-install "$NEO_INSTALL" --jdk-home "$JDK"
python3 -B tests/typed_registry/run_contract.py \
  --jar "$COMPAT" --fixture "$FIXTURE" --server-input "$NEO_INSTALL" \
  --jdk-home "$JDK" --host-oracle "$PINNED_HOST_ORACLE_TSV" \
  --output "$PWD/out/typed-contract"
```

The Contract uses actual FML metadata/scan, an explicit fixed host ClassFile
oracle for offline validation, full-plan clone/ASM checks, typed generics and
business comparisons. It rejects unsupported key/descriptor/generic/Bootstrap,
capture/foreign Handle, bad callback output or local, early consumption, missing
closure, control flow, wrong owner/receiver, duplicate IDs and global Bus. Each
rejection records its phase, reason, no returned plan and unchanged source JAR.
Old 4B/Context/007B and 4C-1 contracts remain required. Offline validation does
not initialize source classes or substitute for a game run.

Capture fresh empty Forge and NeoForge baselines with
`tests/real_mods/food_n_fuel/run_stock.py` using `--host`, `--install`,
`--jdk-home`, and a fresh `--output`. Run native Forge before NeoForge:

```zsh
python3 -B tests/typed_registry/run_runtime.py \
  --host forge --stage treatment --fixture-index "$FIXTURE_INDEX" \
  --install "$FORGE_INSTALL" --jdk-home "$JDK" \
  --baseline "$FORGE_EMPTY_RESULT" --output "$PWD/out/typed-native"
python3 -B tests/typed_registry/run_runtime.py \
  --host neoforge --stage control --fixture-index "$FIXTURE_INDEX" \
  --install "$NEO_INSTALL" --jdk-home "$JDK" --jar "$DUALFORGE_DELIVERY" \
  --native "$NATIVE_CONTROL" --baseline "$NEO_EMPTY_RESULT" \
  --output "$PWD/out/typed-control"
```

Repeat NeoForge with `--stage treatment` and a fresh output. Delivery is named
`DualForge.jar`. Only `DF4C2_REGISTER=false/true` changes; source fixture,
observer, compat and native control bytes stay fixed. All JVM work is serial on
physical copies, using stock launch files and no DualForge JVM properties.
`--stage baseline` is explicitly for the frozen 4C-1 JAR's precise original
ITEM_FIELD_DESCRIPTOR refusal, not for the new compatibility JAR.

Queries verify real Item and Tab IDs, Registry/Holder/object identity, Supplier
counts, official RegisterEvent stack, native coexistence, early unbound Holder
refusal, delayed icon (including cache behavior) and display execution through
actual Minecraft APIs. Server buildContents queries are **not client UI proof**.
Healthy logs compare the complete same-round empty-host error records; a PASS
does not mean zero ERROR. Use `cohort.validate(contract,control,treatment,
register_environment="DF4C2_REGISTER")` in helper_registry to reject mixed builds.

`build_diagnostic.py --neo-install "$NEO_INSTALL" --jdk-home "$JDK" --output
"$FRESH_DIRECTORY"` builds a **separate** NeoForge observer. Its six bridge
negatives use real containers/receiver classes and labeled white-box calls to
private guards. A temporary fixture final-field substitution and temporary Bus
proxy make official native register add one real listener then throw; both
substitutions are restored in finally. Native registeredEventBus=true and bridge
UNCERTAIN must remain, and a retry must be refused. This diagnostic observer is
never mixed into standard C/T and does not establish a security sandbox or
rollback of native side effects. Its exact invocation and index are archived.

FnF uses `--fnf-contract-version typed-v1 --host-oracle "$PINNED_HOST_ORACLE_TSV"`
in `tests/helper_registry/run_contract.py`, with the original fixed JAR. The
exact next full-plan refusal is GLOBAL_EVENT_BUS_UNSUPPORTED. Historical
supplier-v1 receipts remain historical. Normal candidacy still excludes the
unsupported global Bus; FNF_RUNTIME=NOT RUN. No Forge global Event Bus is added.
