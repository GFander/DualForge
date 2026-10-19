# 010A evidence matrix

010B current scope: [controlled official buses/config](dual_bus/README.md),
[source graph](dual_bus/JEI_BUS_SOURCE.md), [reference comparison](dual_bus/REFERENCES.md).
The full original JEI entry still fails preplanning in the new finite constructor
profile; its bus/config runtime and client remain NOT_RUN. The 010A results below
are historical. The old exact-refusal evaluator is not a 010B compatibility test.

| Gate | Evidence type | Result |
|---|---|---|
| 009C final A/B/mouse/config/mixed acceptance | USER_REPORTED_MANUAL_PASS | PASS for the expected009C artifact only; logs not provided |
| Official Forge JEI release freeze | Actual download + all class hashes | PASS |
| Official NeoForge JEI | Binary reference only | PASS input freeze; never substituted |
| Complete release/Forge ABI inventory | STATIC_ANALYSIS | PASS; no mapping approved |
| Original Forge dedicated startup/config/normal stop | DEDICATED_SERVER | PASS |
| Actual JEI instance,5 plugin annotations,service provider types,host recipes | Separate native Forge observer | PASS limited read-only scope |
| Neo discovery/metadata/platform projection/module normalization | REAL_FML_DISCOVERY | PASS through preplanning |
| Neo entry selection/combined preplan | Actual ModLauncher startup | FAIL: Global bus rejected: LISTENER_CONTRACT |
| Client/server modeled planner refusal and atomic publication | MODEL_TEST | PASS28 assertions; refusal is not JEI runtime PASS |
| Forge/Neo SDK class definition and declared member types | No-init JVM | 923 linked/0 unresolved;904 linked/19 unresolved |
| JEI Neo class transformation/definition/construction/lifecycle | Runtime after refused preplan | NOT_RUN |
| JEI client plugin registration/runtime initialization | Actual client | NOT_RUN |
| JEI item/recipe/use/search/bookmark/config/GUI and mixed inputs | Windows/PCL | NOT_RUN / BLOCKED by required ABI |
| 010A client kit | Delivery | BLOCKED; deliberately not built |

Current-build related regression PASS:18 fresh native/Neo/mixed groups, original
Probe and strict Registry/Context/Global/FnF/Clumps/Mouse contracts;106 Python
tests,25 zsh scripts,374 frozen files. The2008 aggregate checks include file
identity/copy checks. Full historical game/client matrices are NOT_RUN.
The new baseline JAR contains the same production entries as009C; no production
change is introduced, and no old Windows PASS is claimed as a new experiment.
