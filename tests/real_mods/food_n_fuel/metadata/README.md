# 007C-1 metadata and admission contracts

`run_metadata.py` compiles `MetadataAdmissionTest.java` and uses the supplied
compat JAR, complete official runtime libraries, and unmodified release/fixture
JARs. It records commands, exits, inputs, byte controls and the compat SHA under
`out/convergence-007c1/metadata-contract/`. All JVM work is serial.

```zsh
python3 -B tests/real_mods/food_n_fuel/metadata/run_metadata.py \
  --jar <new-dualforge.jar> --server-input <official-neoforge-install> \
  --jdk-home <jdk21> --fnf <original-fnf.jar> --dropin <frozen-007b.jar> \
  --multi <frozen-006a.jar> --block <frozen-006b.jar> \
  --clumps <original-clumps.jar> --probe <original-forgeprobe.jar>
```

Contract-only SecureJarHandler exports/opens and a classpath are used here;
these are not the stock runtime command or user installation requirements.
Clumps is parsed statically only. No Clumps function or new constructor/API
compatibility is exercised.

`build_runtime_probe.py --mode parser|selection` additionally takes `--jar`,
`--server-input`, and `--jdk-home`. It builds a diagnostic-only SPI JAR:

- `parser`: resolves/invokes NightConfig through the production Reader's actual
  loader without claiming files. Run against the unchanged pre-task compat JAR.
- `selection`: delegates creation to the production Reader, then records the
  loader FML actually assigned. It has no transformation targets and supplies
  no mod, language provider, dependency override or runtime capability.

Install the diagnostic JAR only in a separate `run_stock.py` physical copy.
The final delivery run contains just the compat and original FnF JAR. These
probes do not establish an unrestricted host-order/provider-priority guarantee.

## Historical test migration

No frozen Forge input or existing Java semantic assertion is modified.
003a C previously generated a metadata-only JAR. It now also generates a real
same-JAR Forge annotation with the same declared modId. A/B remain byte-identical
original Probe copies; C still proves actual FML metadata handoff and dedup.
The ambiguity case now combines a broad loader range with a contradictory
required NeoForge dependency. A broad range alone no longer proves ambiguity;
new contracts explicitly cover unidentified `[0,)` and confirmed `[0,)`.

003b scope controls formerly placed a native annotation owning `identityprobe`
in the Forge-positive JAR. The runner now compiles a temporary foreign-id
variant there, and restores the original native annotation in the native
control. The original identity fixture, control source and all existing scope
assertions remain unchanged. Matching native/Forge ownership conflicts are
also explicitly refused by the new admission contract. Native metadata stays
on the official Reader even when a Forge payload is diagnosed as ambiguous.

## 007C-2 projected-model migration

The original declaration assertions now read a real unprojected `ModFileInfo`
built from the parsed release TOML. They still require both original dependencies
and their exact fields. Separate assertions inspect the live Reader model: one
Minecraft dependency remains, both originals remain in the audit configuration,
and the active Forge platform requirement has a verified projection decision.
Only the offline contract supplies a discovery context; production reads FML's
actual context. See [dependency contracts](../dependency/README.md). Existing
parser/admission negatives and frozen inputs are retained.
