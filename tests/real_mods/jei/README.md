010C 当前入口见 [constructor](constructor/README.md)：Locale/Translator 与布尔配置接口子契约完成有限验证，完整原 JEI 仍严格拒绝，客户端 NOT_RUN。010A/010B 的冻结事实和工具不改。

# Convergence-010A JEI release and startup boundary

010B current scope: [controlled official buses/config](dual_bus/README.md),
[source graph](dual_bus/JEI_BUS_SOURCE.md), [reference comparison](dual_bus/REFERENCES.md).
The full original JEI entry still fails preplanning in the new finite constructor
profile; its bus/config runtime and client remain NOT_RUN. The 010A results below
are historical. The old exact-refusal evaluator is not a 010B compatibility test.

Original Forge input: `jei-1.21.1-forge-19.27.0.340.jar`, official CurseForge
file7420585, 1529402 bytes, SHA-256
`f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5`.
The official NeoForge file7420587 is **binary reference only**, never an installed
replacement. Both original JARs remain unchanged.

Overall **PARTIAL**: native Forge dedicated startup passes; NeoForge + unchanged
DualForge fails at real transformer target planning with
`Global bus rejected: LISTENER_CONTRACT`. No production compatibility is added.
There is no safe 010A Windows delivery ZIP while that required stage is blocked.

Source lives in the SMB worktree; all raw outputs belong to
`/home/albert/Env/DualForge/runs/convergence-010a-jei/`. The task uses the fixed
MC1.21.1 / Forge52.1.0 / Neo21.1.256 / FML4.0.45 / Microsoft JDK21.0.10.
JVM work is serial. Existing 009C client SDKs and 008A measurement inputs may be
reused with hashes; historical results and worlds are not current test results.

`tools/api-atlas/atlas.py usage` scans all923 classes in each JEI JAR; `scan`
records declarations from the fixed official platform SDKs. These retained files
are inputs to `analyze.py`. It checks complete class coverage and SHA, retains
the full call graph, creates211 unique Forge API symbol records with exact
source use sites/descriptors and conservative host-name candidates, and compares
official platform method declarations. No candidate authorizes transformation.
Host module descriptors stay per artifact; ambiguous tool-library providers are
reported without selecting one. A/B/C/D/E/F/G labels describe investigation:
existing conditional support / signature names / semantics / modules / bridge /
ASM / unverified. All proposed JEI API mappings remain unapproved.

`BoundaryContract.java` exercises the actual Reader, FML scan and target planner
in an explicit version/dist model on CLIENT and DEDICATED_SERVER: 28 assertions,
the exact refusal, source preservation and no partial publication. It does not
construct a GAME layer or JEI on NeoForge.

`NativeBaselineObserver.java` is a separate, Forge-only diagnostic input. After
real server startup it reads the actual JEI container instance, FML plugin
annotations, ServiceLoader provider types and host recipe count. It never copies
JEI platform business. Plugin annotation/provider discovery is not client plugin
registration, ServiceLoader provider instantiation, or JEI runtime readiness.

`NoInitLink.java` loads classes without initialization and resolves their declared
member types in separate Forge/Neo SDK classpaths with `-Xverify:all`. Its result
counts linked/unresolved classes; it does not prove ModLauncher module access,
access-transformer application, events, rendering or GUI behavior.

Example orchestration with fresh output directories (the real task's initial
and final receipts distinguish superseded analysis and committed-source runs):

```sh
python3 -B tests/real_mods/jei/analyze.py \
  --run /absolute/SSD/run --output /absolute/SSD/run/new-analysis
python3 -B tests/real_mods/jei/run_boundary.py --run /absolute/SSD/run
```

`run_boundary.py` expects frozen inputs, an unchanged baseline build and ASM
scans under that run, and refuses existing output. The maintained stock server
runner is `tests/real_mods/clumps/run_server.py`; its lifecycle result never
declares JEI GUI functionality PASS. Existing Mouse/Registry/Context/Global/
FnF/Clumps contracts and server experiments run against this round's build.

After those runs, `assess.py --run /absolute/SSD/run --output /absolute/SSD/run/new-assessment.json`
checks the actual fatal chain and artifact bindings independently of successful
refusal/regression assertions. Overall PARTIAL and Neo JEI FAIL are retained;
the generic Mouse regression assessment alone cannot declare JEI compatible.

See [MATRIX](MATRIX.md), [deferred Windows checklist](CLIENT.md), and the
[delivery report](<../../../Dev Manuals/Convergence-010A JEI 交付报告.md>).
