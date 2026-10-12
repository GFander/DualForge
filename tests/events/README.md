# Convergence-004 controlled event dispatch

This test verifies one ordinary, noncancelable, nongeneric custom Forge Event and
one instance listener on the real FML host's global NeoForge bus. It does not add
Forge `post(Event):boolean`, automatic/static subscribers or general event support.

Read [GATE0.md](GATE0.md) for the locked lifecycle and visibility contracts.
Forge sources/resources are in `src/`; native NeoForge driver sources/resources in
`driver/`; unsupported and unclaimed listener controls in `controls/`.
`contract/` holds the standalone Gate 0 public module/loader and switch experiments.
`EventBytecodeTest.java` uses real FML metadata and scan targets to verify the exact
transform, normalized method bytes, authorized differences and atomic rejection.

The driver gets the target module through public LoadingModList/IModFile APIs and
uses the existing FML GAME layer loader. It invokes only the Event's public
constructor. It never invokes the listener or writes EventBus internals.
It posts once inside ServerStartedEvent, after the official global bus start.

Marker scheme C: the listener retains exactly GETSTATIC System.out, LDC String,
INVOKEVIRTUAL PrintStream.println(String), RETURN. During post, the driver temporarily
wraps System.out, forwards the original string unchanged, and records StackWalker
frames when the direct caller is the listener. The top application frame must be
DispatchProbe.onDispatch; deeper frames must include EventBus.post and the driver.
A runtime UUID links that stack record to the driver run. The driver class files
contain no full callback text. The original PrintStream is restored in finally.
No UUID concatenation or stack-capture instructions are added to the Forge listener.

## Serial commands

All JVM invocations, including javac, javap and metadata checks, must be serial.
Use the pinned external inputs described in [tools/UNIX.md](../../tools/UNIX.md).
The runner never downloads dependencies or selects an old compat artifact implicitly.
The Forge fixture classpath contains only the three original Forge dependency JARs.
The driver uses the locked native host dependencies and no compat/Forge JAR.

```zsh
DF_SERVER_INPUT='/workspace/dualforge-cloud/server-base'
DF_FORGE_DEPS='/workspace/DualForge/out/convergence-003b/forge-dependencies'
DF_IDENTITY_JAR='/workspace/DualForge/out/convergence-003b/fixture/20261012-105146-57a9e7aa/identity-check.jar'

zsh tools/entry/forgecompat.zsh --mode build \
  --server-input "$DF_SERVER_INPUT" --output-root out/convergence-004/final-build
DF_COMPAT_JAR='/absolute/new-build/forgecompat-002-0l.jar'

zsh tests/events/run-events.zsh --stage fixture \
  --server-input "$DF_SERVER_INPUT" --forge-dependencies "$DF_FORGE_DEPS"
DF_FORGE_JAR='/absolute/fixture/dispatch-probe.jar'
DF_DRIVER_JAR='/absolute/fixture/dispatch-driver.jar'
DF_REJECTED_JAR='/absolute/fixture/rejected-probe.jar'

zsh tests/events/run-events.zsh --stage contract \
  --jar "$DF_COMPAT_JAR" --fixture "$DF_FORGE_JAR" --rejected-fixture "$DF_REJECTED_JAR"

# Never rebuild any JAR between these two runs. Only DF004_REGISTER changes.
for DF_STAGE in control treatment; do
  zsh tests/events/run-events.zsh --stage "$DF_STAGE" \
    --server-input "$DF_SERVER_INPUT" --jar "$DF_COMPAT_JAR" \
    --fixture "$DF_FORGE_JAR" --driver "$DF_DRIVER_JAR" --rejected-fixture "$DF_REJECTED_JAR" \
    --allow-server-start \
    --java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay
done

zsh tests/events/run-events.zsh --stage regression \
  --server-input "$DF_SERVER_INPUT" --jar "$DF_COMPAT_JAR" \
  --probe /workspace/dualforge-cloud/inputs/forgeprobe.jar \
  --identity-fixture "$DF_IDENTITY_JAR" --forge-dependencies "$DF_FORGE_DEPS" \
  --allow-server-start \
  --java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay
```

Use the actual paths emitted by each stage. Keep the baseline server, frozen A/B
artifacts and history unchanged. JVM dump files live only in each isolated run.
The system property is read in the Forge mod constructor and controls only its
single original Forge EVENT_BUS.register(this) site. inputs.json records the
actual property argument/value/read location. The runner checks observed switch,
registration markers, actual posts and callback counts; it does not infer PASS
from the configured switch. C requires one post/zero callbacks, T one post/one callback.

## Outputs and repeat runs

Every invocation creates a new `out/convergence-004/<stage>/<run-id>/`.
`stage-results.json` selects the latest invoked result for each stage; `summary.json`
aggregates those actual results. Missing stages remain PARTIAL. Failed stages remain
FAIL/NOT RUN until an explicit rerun replaces that stage selection. Historical
failure directories are retained. Do not reuse stage selections from different
artifacts: final aggregation checks identical C/T paths and SHA-256 for all three JARs.

Fixture output includes source copies, compile classpaths, command/exit records,
jar-contents, fingerprints, javap and purity assertions. Contract output includes
before/after class bytes, normalized method-only bytes and full javap.

Dynamic stages include result.json, assertions.json, inputs.json, invocation.json,
artifacts.json and meta.json. The shared harness retains its exact existing layout:
`logs/spike002-0l-treatment/command.json`, meta.json and
`spike002-0l-treatment.{stdout.log,stderr.log,log,debug.log}` (the latter two are
copies of actual latest.log/debug.log). Original files are also in server/logs/.
Runtime transformed classes are in dumps/profile/; bytecode/ validates them after
JVM exit. The harness supports extra test mods and explicit extra JVM arguments;
its original defaults and strict smoke assertions remain unchanged.

Regression invokes the existing A smoke, frozen IdentityProbe, scope, discovery
and metadata/C/D/E/F dependency runners unchanged in assertion strength. Their
result.json paths are indexed in regressions.json. Baseline preservation inputs
must first be recorded under preflight/inputs-before.json as in this task's evidence;
without that snapshot the preservation stage cannot claim PASS. Final preservation
is recorded at out/convergence-004/regression/preservation.json.

Full raw logs, dependencies, class/JAR outputs and worlds are ignored runtime
attachments, not Git content. Only first-party source, runners, reports and evidence
indices are committed. Windows is not validated by these Linux results.
