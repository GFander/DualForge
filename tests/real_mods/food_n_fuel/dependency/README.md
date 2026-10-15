# Controlled Forge platform dependencies (007C-2)

Source-platform version acceptance is separate from Forge runtime/API compatibility.
The reference ABI is Forge 52.1.0, not an installed Forge mod, NeoForge 21.1.256
or the javafml provider's 9999. The production entry checks same-JAR Forge identity
and the real discovery host. Active platform requirements with supported range,
required/optional type and NONE ordering receive an audited internal projection.
Original dependencies remain in `dualforgeOriginalDependencies`, with decisions
in `dualforgePlatformDependencyAudit`; the FML dependency list intentionally differs.
Inactive sides and all non-platform requirements retain official FML semantics.
Native metadata is never projected. No override, fake forge container or JVM flag.

`run_contract.py` takes `--jar --server-input --jdk-home --probe --native --fnf`.
It runs the real FML sorter against controlled memory models and the actual FnF
Reader. Reflection/key-registry setup exists only in the offline contract process;
no GAME layer is created. `ProjectionFeasibilityTest` explicitly marks its synthetic
system metadata experiment DIAGNOSTIC_ONLY. `DiscoveryContextProbe` is likewise
an observer-only diagnostic SPI, absent from final stock matrices.

`build_fixture.py --forge-dependencies <original-003b-deps> --jdk-home <jdk21>
--native <frozen-native-bootstrap.jar>` clean-compiles `fixture/` from the three
pinned Forge 52.1.0/eventbus artifacts, with no NeoForge/DualForge compile inputs.
The A/B entries reuse the original single-bus/empty Event profile; no new event
capability or callback experiment is introduced. Generated TOML variants test
platform ranges, other required/optional constraints, sides and ordering.
Native metadata variants derive into fresh JARs; the original native input and
all old fixtures remain unchanged. Build source, commands, classes, JARs and
fingerprints are saved under `out/convergence-007c2/fixture/`.

`run_matrix.py` takes `--jar` (the final copy named DualForge.jar), `--fixture-index`
(the saved fixture result.json), `--fnf --dropin --observer --native --install
--jdk-home`. It reuses `run_stock.py` for physical copies and serial standard
NeoForge launches. N0/N1/N2/N3, native-only/combined, multi-mod/range refusal,
missing/low ordinary dependencies, optional/sides and ordering each retain raw
lifecycle results and separate gate assertions. A negative gate PASS means the
specific refusal was observed; it never rewrites lifecycle FAIL as mod success.
Healthy stock runs must match this round's empty-host severity counts exactly;
strict zero-ERROR remains a separate limitation. Original FnF and every final
compat/input SHA are checked before and after the matrix.

The 007C-1 metadata contract keeps every original parsing assertion on a real
unprojected ModFileInfo and adds separate assertions on the live projected model.
Its contract-only host snapshot is explicit; the production resolver always uses
FML's real VersionInfo/dist. This is a deliberate migration of the pre-projection
model expectation, not a relaxed parser/identity negative. Frozen source/reports
and third-party bytes are not changed.
