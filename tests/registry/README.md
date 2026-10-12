# Convergence-005: one native Forge Item

Current registration checks PASS; complete 005-S is **PARTIAL**, because the
user's original PNG is unavailable. 005-C is **NOT RUN**: this host has no
display, `/dev/dri` or installed graphical client. No screenshots exist.
The item ID is `registryprobe:ice_ether`, overriding the task's `test_item`.
No replacement texture is generated from the inline preview.

`src/main/java` is original Forge code. `driver/` is a native NeoForge test
mod: it observes the original supplier stack and queries after
ServerStartedEvent, never creates/registers/posts the Item. `ABI.md` records
the exact contracts and pre-change rejection. `RegistryBytecodeTest.java`
uses real FML scanning and checks the controlled transformation, unchanged
supplier, runtime dump, extra registration, wrong fields/bus, unknown lambda,
unclaimed classes and native metadata. Rejected inputs stay byte-identical.

The supported profile is two final static Item fields, one single IEventBus
constructor with the attachment property branch, one get-only accessor, one
zero-capture Item supplier (inert marker then Item/Properties construction),
and one single-item static initializer. No identity whitelist, general
remapper, arbitrary supplier, Block/custom Registry, additional RegistryObject
methods, alternative constructor, or new event ABI is supported.

## Inputs and serial commands

Run from the repo root. All paths must be actual prepared external inputs.
The compiler uses pinned Forge javafmllanguage 52.1.0, Forge universal 52.1.0,
Forge eventbus 6.2.27, and the remapped **pre-patch vanilla Minecraft** jar
from the server installation. Every class in that Minecraft jar must have
no Forge/NeoForge owner references. No NeoForge/compat enters the Forge
compiler classpath. Native driver compilation separately uses the host jars.

```zsh
export JAVA_HOME='/absolute/path/to/msopenjdk-21'
DF_SERVER='/absolute/path/to/server-base'
DF_FORGE='/absolute/path/to/pinned-forge-dependencies'
DF_ORIGINAL='/absolute/path/to/frozen/forgeprobe.jar'
DF_IDENTITY='/absolute/path/to/frozen/identity-check.jar'
DF_EVENT='/absolute/path/to/frozen/dispatch-probe.jar'
DF_EVENT_DRIVER='/absolute/path/to/frozen/dispatch-driver.jar'
DF_EVENT_REJECTED='/absolute/path/to/frozen/rejected-probe.jar'

# Once, before edits. A second preflight refuses to overwrite its snapshot.
zsh tests/registry/run-registry.zsh --stage preflight \
  --server-input "$DF_SERVER" --probe "$DF_ORIGINAL" \
  --identity-fixture "$DF_IDENTITY" --event-fixture "$DF_EVENT" \
  --event-driver "$DF_EVENT_DRIVER" --event-rejected "$DF_EVENT_REJECTED"

zsh tests/registry/run-registry.zsh --stage fixture \
  --server-input "$DF_SERVER" --forge-dependencies "$DF_FORGE"
# Read exact paths from this run's result.json; retain the file names.
DF_FIXTURE='/absolute/path/to/run/registry-probe.jar'
DF_DRIVER='/absolute/path/to/run/registry-driver.jar'
DF_NATIVE='/absolute/path/to/run/native-control.jar'

# Baseline requires a clean build from 004 commit 20e69fae..., in a separate
# worktree, or the recorded original baseline artifact. Do not rebuild the
# current production code and label it pre-change.
DF_BASELINE='/absolute/path/to/004-clean-build/forgecompat-002-0l.jar'
zsh tests/registry/run-registry.zsh --stage baseline \
  --server-input "$DF_SERVER" --jar "$DF_BASELINE" \
  --fixture "$DF_FIXTURE" --driver "$DF_DRIVER" --allow-server-start

zsh tools/entry/forgecompat.zsh --mode build --server-input "$DF_SERVER"
DF_COMPAT='/absolute/path/to/new-build/forgecompat-002-0l.jar'
zsh tests/registry/run-registry.zsh --stage contract \
  --server-input "$DF_SERVER" --jar "$DF_COMPAT" \
  --fixture "$DF_FIXTURE" --native-control "$DF_NATIVE"

# Sequential physical server copies. Same three jars; only property changes.
for DF_STAGE in control treatment; do
  zsh tests/registry/run-registry.zsh --stage "$DF_STAGE" \
    --server-input "$DF_SERVER" --jar "$DF_COMPAT" \
    --fixture "$DF_FIXTURE" --driver "$DF_DRIVER" \
    --native-control "$DF_NATIVE" --allow-server-start
done

zsh tests/registry/run-registry.zsh --stage assets --fixture "$DF_FIXTURE"
zsh tests/registry/run-registry.zsh --stage client --fixture "$DF_FIXTURE"
zsh tests/registry/run-registry.zsh --stage regression \
  --server-input "$DF_SERVER" --jar "$DF_COMPAT" --probe "$DF_ORIGINAL" \
  --identity-fixture "$DF_IDENTITY" --forge-dependencies "$DF_FORGE" \
  --event-fixture "$DF_EVENT" --event-driver "$DF_EVENT_DRIVER" \
  --event-rejected "$DF_EVENT_REJECTED" --allow-server-start

python3 -B tests/registry/test_stage_results.py
zsh -n tests/registry/run-registry.zsh
```

Dynamic cloud commands additionally use
`--java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay`.
Local environments should select their own network settings. Each JVM command
finishes before the next one starts; reuse the existing tools/lib harness,
25565 preflight, physical libraries/config/world copies and graceful stop.

Outputs are ignored `out/convergence-005/<stage>/<unique-run>/`. Commands,
versions, source snapshots, dependency hashes, JAR contents, raw logs, exit
codes and assertions are retained. `stage-results.json` indexes actual runs;
`summary.json` rejects mixed compat/fixture/driver/runtime cohorts. Tests cover
wrong SHA, missing SHA, positional role confusion and stale fixture evidence.
Baseline assertion PASS specifically means the pre-change rejection was
reproduced; it does not mean registry loading succeeded.

## Original texture and client acceptance

Save the original user-supplied PNG bytes as
`src/main/resources/assets/registryprobe/textures/item/ice_ether.png` once
available. Do not regenerate from the preview. Static checks require a 32×32
RGBA PNG with transparent and visible pixels, `minecraft:item/generated`
model and matching `item.registryprobe.ice_ether` keys. Model/en/zh JSONs are
already packaged. A missing PNG leaves assets NOT RUN and complete 005-S
PARTIAL. PNG validation uses Pillow; other runner operations use the standard
library. After adding the real resource, rebuild the native fixture and rerun
contract, C/T and assets with its new SHA; old results cannot be mixed.

`client` only records environment and NOT RUN, not a graphical acceptance.
After complete 005-S PASS, use Minecraft 1.21.1 + NeoForge 21.1.256 client
with the **same** Forge fixture and compat (DF005_REGISTER=true). In a fresh
isolated test world, run `/give @p registryprobe:ice_ether 1`, verify inventory
and hand-held rendering, no missing model/texture or purple/black fallback,
and switch English/Simplified Chinese to verify both names. Save actual
client logs, commands, artifact hashes and real screenshots externally.
Dedicated Server output and resource presence cannot prove any of these.
