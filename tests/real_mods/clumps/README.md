# Convergence-008A Clumps

This directory holds independent original-release evidence, a renamed native
Forge publisher fixture, and refusal contracts for the finite event initializer.
Third-party JARs and runtime evidence stay outside Git. See [MATRIX](MATRIX.md)
and the [delivery report](../../../Dev%20Manuals/Convergence-008A%20Clumps%20交付报告.md).

All JVM work is serial. Use the fixed Java 21 installation and fresh physical
server copies under `/home/albert/Env/DualForge/runs/`. Do not open historical
worlds or change the original Clumps release.

The original Clumps input is 18226 bytes, SHA-256
`e1de425ddd6f2b5c195c37a008d9fbc2c00c32fe1e214d76c63ab35446d13c19`.
Its provenance is the user's local input; the supplied Windows crash ZIPs were
not found locally. The original Forge/NeoForge event ABI and all 15 release
classes were inspected with the fixed JDK before production changes.

Example commands from the repository root (choose fresh output directories):

```sh
DF_ENV=/home/albert/Env/DualForge
DF_RUN="$DF_ENV/runs/new-008a-run"
DF_JDK="$DF_ENV/jdks/msjdk-21.0.10"
python3 -B tests/real_mods/clumps/build_compat.py \
  --server-input "$DF_ENV/neoforge-21.1.256" --jdk-home "$DF_JDK" --output "$DF_RUN/build"
python3 -B tests/real_mods/clumps/run_server.py \
  --host forge --install "$DF_ENV/forge-52.1.0" --jdk-home "$DF_JDK" \
  --output "$DF_RUN/forge" --mod "$DF_ENV/inputs/Clumps-forge-1.21.1-19.0.0.1.jar" --orbs
python3 -B tests/real_mods/clumps/run_server.py \
  --host neoforge --install "$DF_ENV/neoforge-21.1.256" --jdk-home "$DF_JDK" \
  --output "$DF_RUN/neo" --mod "$DF_RUN/build/dualforge.jar" \
  --mod "$DF_ENV/inputs/Clumps-forge-1.21.1-19.0.0.1.jar" --orbs
python3 -B tests/real_mods/clumps/run_server.py \
  --host neoforge --install "$DF_ENV/neoforge-21.1.256" --jdk-home "$DF_JDK" \
  --output "$DF_RUN/reloaded" --mod "$DF_RUN/build/dualforge.jar" \
  --mod "$DF_ENV/inputs/Clumps-forge-1.21.1-19.0.0.1.jar" --reload-from "$DF_RUN/neo"
```

`build_compat.py` delegates to the maintained production compiler/packager. It
changes only the output placement to SSD. `run_server.py` uses physical copies of
the official installation, stock `sh run.sh nogui`, captures the actual JVM
command, binds loopback, and keeps every world/log/input. `--orbs` summons ordinary
orbs with initial values 3/7/11, advances exactly 400 official world ticks and reads
their NBT. It never assigns Count, clumpedMap, age or merge output. `--reload-from`
copies only a successful sibling experiment with identical mods; it verifies
copied bytes and preservation of the source world. Lifecycle PASS is separate
from the comparisons in `assess.py`.

`build_fixture.py` uses Forge-only inputs. `build_observer.py --host forge` must
precede the corresponding Neo observer/run; add `--independent-fixture` for the
renamed fixture. Observers invoke the real installed callback and ServiceLoader
provider with explicitly synthetic null player/orb arguments. They register and
remove native listeners, checking cancellation and mutable event object identity.
They do not spawn a player, give XP, simulate pickup, or exercise Mending. Standard
and observed game cohorts remain separate.

`run_contract.py` needs `--jar --clumps --fixture --neo-install --jdk-home --output`.
It covers original/renamed source shapes, rejected constructors/bootstrap/API/
ownership/event bodies, original ClassNode preservation, changed-input SHA refusal,
and diagnostic transform-branch failures. Diagnostic injection is not a live FML
or Module Layer test.

`run_existing_contracts.py` uses the SHA-verified public 4C-3 archive's original
Probe/native/helper/Global fixtures, not reconstructed substitutes.
`run_profile_regression.py` builds unchanged active Normal/Legacy/Block/Context
fixture sources as a **new cohort** for the original contracts; their historical
dynamic/client results are not inherited. Existing scripts keep their paths and
bytes. `--fnf-furnace` invokes the unchanged 4C-5 driver directly, with this round's
new compat JAR; it does not bypass or rewrite the historical CLI's frozen SHA.

Player pickup, Mending, client boot/manual acceptance and player persistence stay
NOT_RUN until actual user evidence. [Client instructions](CLIENT.md) specify the
single-factor Windows test and feedback requirements. No raw evidence upload is
authorized by this test code/PR task.
