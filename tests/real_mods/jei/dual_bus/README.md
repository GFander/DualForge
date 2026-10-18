# 010B official buses and boolean configuration

The new production profile is a **finite three-class constructor/route/config
closure**, selected by two proven bus acquisitions and a constructor taking two
bus arguments. It accepts arbitrary source names, mod IDs, setting names/defaults
and supported callback methods; it contains no fixture/JEI identity allowlist.
It does not authorize the full JEI entry. See [JEI source graph](JEI_BUS_SOURCE.md)
and [reference comparison](REFERENCES.md).

The two Forge-only fixtures are `busalpha`/`org.example.busalpha` (COMMON) and
`busbeta`/`org.example.busbeta` (SERVER). Both use the real Game Bus and their
own real Mod Bus. The original SHA-identical Forge JARs run on both hosts.
Beta's death listener opts into `receiveCanceled`; Alpha's does not. No native
Neo JEI JAR, preconverted Forge input, JVM agent or business launch flag is used.
The original Probe and previous task inputs remain frozen.

The adapter recognizes only `FMLCommonSetupEvent`, `ServerStartedEvent` and
`LivingDeathEvent`; priority NORMAL; explicit Class/Consumer; bounded boolean
Builder/Spec construction. Actual host classes/implementation bytes are checked.
Source cancellation calls become the real Neo `ICancellableEvent` operations.
The callback body and original captured Consumer are preserved. Source `post`,
Event.Result, custom buses, arbitrary priorities/specs/bootstrap/entry effects,
client lifecycle and JEI WeakConsumer subscriptions remain unsupported.
`CUSTOM_BUS_SUPPORT=NOT_IMPLEMENTED`.

`ForgeBusIdentity` binds actual game/mod objects, owning ModContainer/ModFile,
module, classloader and GAME layer. `ForgeEventSemantics` independently validates
the event/options. `ForgeDualBusPlan` validates the whole source closure and
renders clone outputs before transformer publication; original SHA and normalized
input are checked again per class. `ForgeDualBusBridge` is the host effect layer.
Its per-owner sessions are held through weak static references and private live
entry/router anchors; the static cache cannot retain an owner through its value.
Configuration has a separate once-only flag; listener identity/order records
cannot select another config owner. Re-entry/late registration and retry after
UNCERTAIN refuse. Host exceptions propagate; no rollback is promised.

The source Forge IConfigSpec is never cast into a Neo spec. The verified real
Forge Builder `push/comment/define(boolean)/pop/build` sequence is rewritten to
construct the actual Neo ModConfigSpec. Its original Supplier<Boolean> fields
and getters keep the native BooleanValue. Type constants retain their exact
COMMON/SERVER/CLIENT values. COMMON and SERVER are actually exercised; CLIENT
loading/saving is **NOT_RUN**. A separately native observer SERVER spec verifies
the host's real default path: Forge world/serverconfig, this Neo dedicated host
config/. A claim of identical Forge per-world path policy is **not** made.

Evidence is split explicitly:

- `PlanContract`: bytecode plan/model, source preservation and negative mutations;
  no GAME layer/constructor claim.
- `NativeBusContract`: **ISOLATED_NATIVE_API**, real platform Bus/Event objects;
  cancellation, thread, order, parameter identity, exception and unregister ABI.
- `Observer`: separate native diagnostic mod. Real FML setup and server-start
  callbacks are recorded before diagnostic extra posts. A-only/B-only Mod Bus
  posts verify routing isolation; they are not extra FML lifecycle stages.
- `OwnerModels`: **ISOLATED_MODEL_WITH_LIVE_OWNER**; deliberately changes/restores
  diagnostic identity fields and injects faults into the host-operation core.
  It bypasses the public caller path. Before/after-effect faults retain real native
  effects and mark UNCERTAIN. This is not a security sandbox or full-chain fault test.
- The positive callback cancellation test temporarily substitutes a diagnostic
  Supplier and invokes the original Consumer. Null-entity death posts are
  diagnostic. Actual mob death/gameplay and all GUI/client checks are **NOT_RUN**.
- `run_live.py`: real stock startup/normal stop, default values, physical copying
  of the stopped world/config, edits to official TOML, restart, then restart again
  without edits. It verifies native SERVER control locations and the two owners'
  independent values. `run_restart.py` changes only pre-launch files in a fresh
  SSD experiment; the official JVM command is unchanged.

Use fresh absolute SSD destinations; every tool refuses existing outputs. Fixed
MC1.21.1 / Forge52.1.0 / Neo21.1.256 / Microsoft JDK21.0.10. JVMs run serially.
The SDK remains under `~/Env/DualForge/runs/convergence-009c-20261017/client-abi`.

```sh
python3 -B tests/real_mods/jei/dual_bus/build_fixture.py --output /absolute/SSD/fixture
python3 -B tests/real_mods/jei/dual_bus/build_observer.py --output /absolute/SSD/observer
python3 -B tests/real_mods/jei/dual_bus/run_contract.py \
  --jar /absolute/SSD/dualforge.jar --fixtures /absolute/SSD/fixture --output /absolute/SSD/contracts
python3 -B tests/real_mods/jei/dual_bus/run_live.py \
  --jar /absolute/SSD/dualforge.jar --fixtures /absolute/SSD/fixture \
  --observers /absolute/SSD/observer --output /absolute/SSD/live
python3 -B tests/real_mods/jei/dual_bus/collect_abi.py --output /absolute/SSD/abi
```

The 010A BoundaryContract/run_boundary tool remains a frozen historical exact
refusal evaluator. It must not be used to assert the new candidate's error.
Current 010B JEI startup is independently rerun and its actual preplan failure
retained. Neither static source graphs nor successful negative tests imply JEI
class construction or client compatibility. No 010B JEI client ZIP is produced.
