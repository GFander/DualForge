# Convergence-003b controlled entry identity

The production path obtains Forge entry names from the current FML scan's
type-level `net.minecraftforge.fml.common.Mod`, whose `value` equals the owning
metadata modId. It reuses the unchanged 003a candidate admission policy before
selecting any Forge entries. Native metadata keeps the builtin javafml path.
ModLauncher calls `targets()` after FML `completeScan`, before the GAME layer;
`IModFile.getScanResult()` waits for the existing background scan. No alternate
scanner, class loader, module descriptor or layer is installed.

The only targets are matched entries and the listener's direct Forge Event
subclass found in the **same mod file's** FML scan. Sharing a package, constructor
descriptor or Event superclass does not establish ownership. The transformer
also refuses unselected classes if directly offered one.

The supported profile stays limited to the original one-parameter
`(Lnet/minecraftforge/eventbus/api/IEventBus;)V` constructor, the single exact
`MinecraftForge.EVENT_BUS.register(this)` sequence, a public nonstatic empty
instance listener with one default `@SubscribeEvent`, and its final, fieldless,
interface-free Event with only its trivial superclass constructor. The entry
still has exactly its constructor and listener methods. Owner/API descriptors,
rewrite flags and `ForgeEmptyRegistration` behavior are unchanged. The frozen
legacy zero-method ForgeProbe shortcut is unused by this nonempty profile and
is not generalized. A successful real register return does not verify event
delivery. No new constructor ABI, automatic subscriber semantics, arbitrary
real mod compatibility or simultaneous multi-Forge startup is claimed.

`src/` is native Forge source: `identityprobe`,
`com.example.identitycheck.IdentityProbe`, `IdentityEvent`, `identity-check.jar`.
Its two unique output markers bracket the original register behavior. The
fixture build uses **only** the original three Forge compile artifacts, pinned
to the hashes in `run_identity.py`; neither the compat shim nor NeoForge is on
its compile classpath. The JAR is checked for original Forge annotations,
constructor and API references, with no NeoForge class references. Source
copies, command/exit logs, dependency fingerprints, JAR entry fingerprints and
javap output are saved in fresh `out/convergence-003b/fixture/` directories.

Prepare these external artifacts in a chosen dependency directory:

| Artifact | Official Maven URL |
|---|---|
| javafmllanguage 1.21.1-52.1.0 | https://maven.minecraftforge.net/net/minecraftforge/javafmllanguage/1.21.1-52.1.0/javafmllanguage-1.21.1-52.1.0.jar |
| eventbus 6.2.27 | https://maven.minecraftforge.net/net/minecraftforge/eventbus/6.2.27/eventbus-6.2.27.jar |
| forge universal 1.21.1-52.1.0 | https://maven.minecraftforge.net/net/minecraftforge/forge/1.21.1-52.1.0/forge-1.21.1-52.1.0-universal.jar |

The current run recorded downloads and exact historical hash matches in
`out/convergence-003b/forge-dependencies/downloads.json`. The official endpoint
rejected Python's default User-Agent with Cloudflare 1010; the normal
`Apache-Maven/3.8.5` User-Agent succeeded through the inherited HTTPS proxy and
CA. No artifact or TLS verification was relaxed.

From the checkout, with the existing complete server, original Probe, world,
JDK21.0.10+7-LTS and Linux isolation inputs, run **every JVM command serially**:

```bash
# Capture a clean starting compat build before editing production code.
zsh tools/forgecompat.zsh --mode build \
  --server-input /workspace/dualforge-cloud/server-base \
  --output-root out/convergence-003b/before-build

zsh tests/identity/run-identity.zsh --stage fixture \
  --forge-dependencies /workspace/DualForge/out/convergence-003b/forge-dependencies

# Replace both paths with the actual preceding result.json paths.
zsh tests/identity/run-identity.zsh --stage before \
  --fixture /absolute/fixture/identity-check.jar \
  --jar /absolute/before-build/forgecompat-002-0l.jar \
  --allow-server-start \
  --java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay

# After the identity fix, build again and use its exact artifact for all checks.
zsh tools/forgecompat.zsh --mode build \
  --server-input /workspace/dualforge-cloud/server-base \
  --output-root out/convergence-003b/after-build
zsh tests/identity/run-identity.zsh --stage after \
  --fixture /absolute/fixture/identity-check.jar \
  --jar /absolute/after-build/forgecompat-002-0l.jar \
  --allow-server-start \
  --java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay
zsh tests/identity/run-scope.zsh \
  --jar /absolute/after-build/forgecompat-002-0l.jar \
  --fixture /absolute/fixture/identity-check.jar \
  --forge-dependencies /workspace/DualForge/out/convergence-003b/forge-dependencies

zsh tools/forgecompat.zsh --mode smoke \
  --server-input /workspace/dualforge-cloud/server-base \
  --probe /workspace/dualforge-cloud/inputs/forgeprobe.jar \
  --jar /absolute/after-build/forgecompat-002-0l.jar \
  --output-root out/convergence-003b/original-smoke --allow-server-start \
  --java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay
zsh tests/discovery/run-discovery.zsh /absolute/after-build/forgecompat-002-0l.jar \
  /workspace/dualforge-cloud/server-base /workspace/dualforge-cloud/inputs/forgeprobe.jar
zsh tools/test-forge-dependencies.zsh --stage after \
  --server-input /workspace/dualforge-cloud/server-base \
  --probe /workspace/dualforge-cloud/inputs/forgeprobe.jar \
  --compat-jar /absolute/after-build/forgecompat-002-0l.jar \
  --output-root out/convergence-003b/dependencies --allow-server-start \
  --java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay
```

`before` verifies the recorded original FML unsupported Forge constructor
argument rejection, rather than treating its JVM exit code (0) as success.
It requires the saved starting build; running it with fixed code is expected to
fail its reproduction assertions. `after` requires complete load, exact installed
hashes, single discovery, entry/event rewrites, unique markers, native delegation,
Done/stop/exit0, empty runtime stderr and no ERROR/FATAL/dump FAIL/crash.

The scope runner uses actual FML Readers, metadata, scanning and LoadingModList
with fresh fixture JARs. Controls add Ordinary, UnownedEntry, UnrelatedEvent and
NativeControl; the native dual-metadata fixture retains identical Forge entry
bytes. It checks exact target sets and saves byte-identical before/after
canonical ASM output for unclaimed classes. It never loads or initializes those
candidate classes and is not a multi-mod server test. The original discovery
runner retains its `out/convergence-003a/discovery/` output root; its current input
fingerprint identifies the new 003b compat artifact.

Dynamic runs reuse the verified physical-copy, loopback, official Unix named
module argument file, original flags, local dump paths and public-key relay
measures. A and B are never installed together. Historical inputs, worlds and
evidence are checked by the 003b preservation manifest. Only the zsh entrypoints are maintained; historical platform claims remain
in the original reports.
