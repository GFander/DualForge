# Fixed-version API Atlas (007C-4A)

Independent Java 21/ASM research for Forge 52.1.0 → NeoForge 21.1.256,
Minecraft 1.21.1 / FML 4.0.45. No transformer service or game server is started.
Production profiles, original JARs and runtime configuration are read-only.

Use fresh output directories. From the repository root:

```zsh
DF_JDK='/absolute/path/to/jdk21'
DF_FORGE='/absolute/path/to/forge-52.1.0-installation'
DF_NEO='/absolute/path/to/neoforge-21.1.256-installation'
DF_OUT="$PWD/out/convergence-007c4a/new-run"
python3 -B tools/api-atlas/inputs.py --forge-install "$DF_FORGE" \
  --neo-install "$DF_NEO" --java-home "$DF_JDK" --output "$DF_OUT/inputs"
python3 -B tools/api-atlas/verify_inputs.py --inputs-directory "$DF_OUT/inputs" \
  --forge-installer /absolute/path/to/forge-1.21.1-52.1.0-installer.jar \
  --neo-installer /absolute/path/to/neoforge-21.1.256-installer.jar \
  --output "$DF_OUT/input-verification.json"
python3 -B tools/api-atlas/atlas.py scan --java-home "$DF_JDK" \
  --libraries "$DF_NEO/libraries" --inputs "$DF_OUT/inputs/scan.tsv" \
  --output "$DF_OUT/scan.jsonl.gz" --work "$DF_OUT/scan-tool"
python3 -B tools/api-atlas/analyze.py compare --scan "$DF_OUT/scan.jsonl.gz" \
  --manifest "$DF_OUT/inputs/manifest.json" --output "$DF_OUT/atlas"
```

ASM 9.10.1 and Gson 2.10.1 come from the fixed installation. `manifest.json`
is portable; `local-paths.json` and TSVs record local paths. All installed JARs
are scanned, while official bootstrap/argument files determine active resources.
Java base is separately attributed. Complete patched client inputs are missing:
client Minecraft ABI remains `PARTIAL / INPUT_MISSING`. ClassFile declarations
do not include subsequent runtime mixin/access-transformer effects.

Compressed JSONL retains exact descriptors and separate generic signatures,
annotations, records, nests, modules, permitted subclasses and member flags.
IEEE bits preserve floating constants. Stable sorting and gzip headers exclude
generated timestamps and workspace paths. Each Java command saves argv, exit,
stdout and stderr. Existing output is refused, preserving failed attempts.

`all-duplicate-origins.jsonl.gz` retains all origins, including inactive files.
`duplicates.json` records active providers, Java 21 variants, per-module
descriptors and official Minecraft patch overlays. Unresolved provider conflicts
are excluded from the usable index, never silently overwritten.

The versioned [candidate schema](mappings/candidate.schema.json) separates
discovery from compatibility. Owner/name/descriptor/opcode changes, identical
symbols and similar class structure remain candidates. Conflicts are ambiguous;
unresolved does not prove absence. Public/protected counts use ClassFile and
enclosing-class access, not a promise that every symbol is exported by JPMS or a
supported public platform API. Internal declarations are retained separately.

For read-only usage analysis, create a TSV line as
`artifact-id<TAB>/absolute/path/to/original.jar`:

```zsh
python3 -B tools/api-atlas/atlas.py usage --java-home "$DF_JDK" \
  --libraries "$DF_NEO/libraries" --inputs /absolute/path/to/mod.tsv \
  --output "$DF_OUT/mod-raw.jsonl.gz" --work "$DF_OUT/mod-scan-tool"
python3 -B tools/api-atlas/analyze.py usage --scan "$DF_OUT/mod-raw.jsonl.gz" \
  --atlas "$DF_OUT/atlas" --mcp /absolute/path/to/fixed-mcp-config.zip \
  --mojmap /absolute/path/to/server-1.21.1-mappings.tsrg --output "$DF_OUT/mod"
```

Outputs separate use points, unique symbols, internal calls, bootstrap handles,
dynamic constants, strings and inherited override contracts. Reflection strings
are blind-spot evidence. `ALREADY_COMPATIBLE` means observed static ABI presence
with member opcode/access checks, not lifecycle or game behavior.

`existing.py --root <repo> --atlas <atlas> --production <compat-usage.jsonl.gz>
--output <rules.json>` reads an ASM usage scan of the unchanged compat JAR and
records exact descriptors, source fingerprints, scopes and historical contracts.
Entry-only support never grants FnF helper transformation automatically.

The [offline tests](../../tests/api_atlas/README.md) require explicit
`OFFLINE_STATIC_EXPERIMENT` plans. `OfflinePrototype` checks fixed source/target
members, opcodes, type dataflow and cross-class closure before editing clones or
publishing outputs. Originals and unclaimed classes retain their bytes. Frames
are remapped without recomputation; unknown hierarchy never defaults to Object.
Only Item-typed field/create/get and holder return closure are demonstrated.
Unknown handles/indy/condy in claimed classes, other Registry generics and
explicit JPMS are refused. Context/Bus conversion remains a future runtime shim;
BusView is not a native bus. FnF output is refused, not published as usable.

Full generated data stays under ignored `out/`, with fingerprints and an external
evidence archive. See [delivery](<../../Dev Manuals/Convergence-007C-4A 交付报告.md>)
for actual results, limitations and the separate 007C-4B proposal.

## 007C-4C-0: read-only FnF gap analysis

`usage` now also retains instruction operands, labels/frames/locals and exception tables in each method's `code` section. Reference counting is unchanged. No input class is defined.

After a fresh `atlas.py usage` scan of the fixed original release:

```zsh
python3 -B tools/api-atlas/fnf_gap.py \
  --scan /absolute/new-fnf-usage.jsonl.gz \
  --atlas /absolute/fixed-available-server-atlas \
  --fnf /absolute/original-fnf-forge-1.3-1.21.x.jar \
  --output "$PWD/out/convergence-007c4c0/new-analysis"
```

This reuses `analyze.usage` and the existing ABI resolver. It verifies complete ClassFile inventory and class SHA against the original JAR, keeps unresolved symbols, classifies all bootstrap sites, and emits six versioned datasets. [Schema](fnf-gap.schema.json) defines the envelope/data contracts; [design decisions](fnf-gap-design.json) generate the frontier and dependency DAG. Classification describes structure and conditional boundaries; it approves no Supplier business code or production mapping. The release SHA is a research input guard, never a production selector.

The six derived datasets and report are versioned under [Dev Manuals/Convergence-007C-4C-0](../../Dev%20Manuals/Convergence-007C-4C-0/). Original ASM scans, javap, commands/logs and binary inputs remain under ignored out/. Existing 4A ABI data is reused with exact dataset hashes and freshly reverified official artifact checksums; the server view still does not supply a full patched client ABI.
