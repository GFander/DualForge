# Convergence-009C Mouse Tweaks

This directory verifies the original Forge 2.26 release and a separate
`gestureinput` Forge fixture. The compatibility rule uses constructor, listener,
initialization, same-file ownership and factory dataflow; no Mouse Tweaks modId,
package, filename or release SHA is a production admission rule.

The input is `/home/albert/Env/DualForge/inputs/MouseTweaks-forge-mc1.21-2.26.jar`,
73287 bytes, SHA-256 `750dd1806a9e67ba23be58403648293bc1e936a068ba6d8184aac88fc3e0bc62`.
All new outputs belong on SSD under
`/home/albert/Env/DualForge/runs/convergence-009c-20261017/`; no third-party JAR,
world, dependency or full log belongs in Git. JVM work is serial.

`ProfileFailure.java` reproduces baseline entry selection using the actual class;
it does not reproduce a Linux client boot. `ClientProfileContract.java` checks
both input identities, constructor/order/annotation/event/config/owner refusals,
source SHA binding, unchanged callers and actual transformer branches.
`ClientOwnershipContract.java` uses the actual Reader, FML scan and target planner
with explicitly modeled version/dist state. It checks metadata, cross-ModFile and
explicit-module refusals, and no client plan on a server. No GAME layer or mod
instance is constructed by that contract.

`build_fixture.py` requires the complete original **Forge** client libraries;
server installs contain only some patched client classes and cannot substitute
for the complete client ABI. Both official installers may prepare isolated
client dependencies on SSD without starting a game or using a launcher account.

`NativeApiHarness.java` posts the four native events through the real official
bus, verifies once-only dispatch, button/coordinates/drag/scroll data, cancellation,
default cancellation filtering and unregister. On Forge it applies the official
EventSubclassTransformer to in-memory event definitions, as the normal launch
plugin does. Original SDK/mod JARs remain unchanged. Fixture and Screen objects
explicitly bypass their constructors; no GUI is initialized or operated. The Neo
factory test adds a diagnostic in-memory accessor for the actual generated
metafactory callsite and exercises the native ModContainer extension API. Private
bridge state-core diagnostics do not validate its public FML/Module Layer path.

`ClientLinkContract.java` checks exact member descriptors/access against the
complete Neo client files, all six original Mixin selectors, the Minecraft
instance assignment before client mod loading, and JVM verification of all 21
release classes without initialization. This is not Mixin application or GUI PASS.

`OriginalInitContract.java` separately injects a constructor-free Minecraft
singleton whose game directory is a fresh SSD folder. It executes the unchanged
original Main.initialize/config IO and the actual original ConfigScreen
constructor through both generated callsites, checking once-only initialization,
fresh Screen creation and parent identity. No Minecraft/entry constructor,
Screen.init, rendering, window or mouse operation runs in this explicit model.

Example commands (use fresh output directories):

```sh
DF_RUN=/home/albert/Env/DualForge/runs/new-009c-run
DF_COMPAT=/absolute/path/to/dualforge.jar
DF_FORGE_CLIENT=/absolute/path/to/isolated-forge-client/libraries
DF_NEO_CLIENT=/absolute/path/to/isolated-neo-client/libraries
python3 -B tests/real_mods/mouse_tweaks/build_fixture.py \
  --client-libraries "$DF_FORGE_CLIENT" --output "$DF_RUN/fixture"
python3 -B tests/real_mods/mouse_tweaks/run_contract.py \
  --jar "$DF_COMPAT" --mouse /home/albert/Env/DualForge/inputs/MouseTweaks-forge-mc1.21-2.26.jar \
  --fixture "$DF_RUN/fixture/gesture-input.jar" --output "$DF_RUN/contract"
python3 -B tests/real_mods/mouse_tweaks/run_api.py --host forge \
  --jar "$DF_COMPAT" --fixture "$DF_RUN/fixture/gesture-input.jar" \
  --client-libraries "$DF_FORGE_CLIENT" --output "$DF_RUN/api-forge"
python3 -B tests/real_mods/mouse_tweaks/run_api.py --host neo \
  --jar "$DF_COMPAT" --fixture "$DF_RUN/fixture/gesture-input.jar" \
  --client-libraries "$DF_NEO_CLIENT" --transformed "$DF_RUN/contract/transformed/gestureinput" \
  --output "$DF_RUN/api-neo"
```

`run_links.py` accepts `--jar --mouse --transformed --client-libraries --output`.
`run_regression.py --jar --output --inputs-008a` uses unchanged prior tools and
SHA-checked original measurement inputs for fresh native/Neo server runs. It
never inherits an old result/world. `run_mixed.py` extends only the console
measurement sequence (FnF furnaces then real orbs), without editing the old runner
or injecting JVM/business options.
`run_checked_tool.py` changes only legacy diagnostic JVM working directories to
SSD and records each requested/actual cwd; original test files remain unchanged.

The four Windows GUI/client gates remain NOT_RUN until user feedback. See
[MATRIX](MATRIX.md) and the single [Windows checklist](CLIENT.md).
