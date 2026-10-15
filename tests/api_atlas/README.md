# API Atlas tests

Independent offline tools; production, runtime configuration, original FnF and
historical fixtures are read-only. No test starts Forge or NeoForge.

Pure Python checks, also included in the existing CI unit-test step:

```zsh
python3 -B -m unittest discover -s tests/api_atlas -p 'test_*.py' -v
```

They cover descriptors, inheritance and private/constructor boundaries,
conservative interface ambiguity, access, duplicate providers, overlays,
multi-release selection, determinism, naming overloads, unknown usage and rule
approval. The ASM/JVM contract is run explicitly against external official inputs:

```zsh
python3 -B tests/api_atlas/run_checks.py \
  --java-home /absolute/path/to/jdk21 \
  --libraries /absolute/path/to/neoforge-install/libraries \
  --inputs-directory /absolute/path/to/frozen-input-directory \
  --atlas-directory /absolute/path/to/generated-atlas \
  --fnf /absolute/path/to/unchanged-fnf-forge-1.3-1.21.x.jar \
  --output "$PWD/out/convergence-007c4a/new-offline-checks"
```

Prepare inputs with [the tool CLI](../../tools/api-atlas/README.md). Generate each
`forge-oracle.tsv` / `neoforge-oracle.tsv` from its selected `*-abi.jsonl.gz`,
resolving artifact IDs through `local-paths.json`: one line per class as
`internal-name<TAB>absolute-jar-path<TAB>jar-member`. Do not select duplicate
providers independently. The evidence contains exact TSVs and full task commands.

The driver compiles AtlasProbe/Peer/Untouched/VerifyFixture from saved sources
using official Forge inputs only, then saves source/class/JAR fingerprints.
PrototypeContract checks exact API migration, cross-class returns and generic
arity, refusal and original-node atomicity. ScannerContract covers modules,
bridge/synthetic flags and ConstantDynamic metadata; Shapes.java supplies actual
Java 21 nested generics, records, sealed classes and annotations.

ASM checks and standalone `java -Xverify:all` class loading have separate receipts.
The latter loads fixture classes and checks a null receipt; Registry creation,
events, suppliers and game lifecycle are NOT RUN. FnF is only inventoried and
rejected. A partial Registry chain remains PARTIAL despite successful controlled
bytecode output. Complete patched client inputs are a separate missing gate.

## FnF closure evidence (007C-4C-0)

`test_fnf_gap.py` covers descriptive supplier categories, unknown bootstrap/descriptor preservation, CFG branch/exception edges, conditional plan refusal, and the source BusView return ABI. `ScannerContract` additionally checks serialized instruction operands and exception targets (14 assertions total). The same existing CI unit-test step discovers the Python tests. All actual Java checks remain serial.

The [report](../../Dev%20Manuals/Convergence-007C-4C-0%20FnF兼容缺口报告.md) separates normal candidate exclusion, forced read-only plan rejection, static symbols, proposed rules and unexecuted gameplay. No test starts FnF or changes its original JAR.
