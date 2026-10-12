# Convergence-002 Unix entrypoints

The current entrypoints are `forgecompat.zsh`, `test-forge-dependencies.zsh`,
and `../spike000/run-server.zsh`. They retain the original PowerShell files.
zsh launches the Python 3 standard-library implementation for JSON, ZIP hashes,
and process/log capture. No new build framework or Java compatibility changes.
Historical Spike scripts are not migrated or executed.

Required tools: zsh, Python 3.11+, Microsoft JDK **21.0.10+7-LTS**; `pgrep`
is additionally required before an explicitly requested server run.
`JAVA_HOME` or `--jdk-home` selects the JDK. Maven downloads use the platform
HTTPS proxy and CA, with authoritative artifact checksum verification retained.

From the checkout root:

```zsh
zsh tools/prepare-unix.zsh --server-input /workspace/dualforge-cloud/server-base
zsh tools/forgecompat.zsh --mode build \
  --jdk-home /workspace/dualforge-cloud/tools/msjdk/usr/lib/jvm/msopenjdk-21-amd64 \
  --server-input /workspace/dualforge-cloud/server-base

# Supply the actual newly built JAR; do not rename it.
zsh tools/test-forge-dependencies.zsh --stage after --boundary-only \
  --server-input /workspace/dualforge-cloud/server-base \
  --probe /workspace/dualforge-cloud/inputs/forgeprobe.jar \
  --compat-jar out/unix/build/<actual-run>/forgecompat-002-0l.jar
```

Preparation downloads only the eight fixed compile JARs, not a complete server
installation. Boundary tests additionally need the complete runtime libraries
and the original fixed Probe. Its required SHA-256 is
`A774D2F737B17F089046014AB55A53767DBAD008CD44DCCE52C727B87E559AE8`.
Missing or incorrect Probe is rejected; no rebuild or substitution is allowed.

All outputs go to fresh directories below checkout `out/`; use `--output-root`
to choose another subdirectory there. Original installation inputs are copied,
not modified. Inspect current `result.json`, command logs, exit records and
input fingerprints; prior reports are not evidence of a current run.
Build/regression exit codes: 0 PASS, 10 BUILD, 20 STARTUP, 30 ASSERTION, 40 NOT RUN;
a successful build followed by unmet regression prerequisites is PARTIAL.
The independent dependency entrypoint retains failure exit code 1; unmet
prerequisites exit 40. The standalone harness uses 20 for unsuccessful startup.

## Authorized regression

`--mode regress`, `--mode smoke`, dynamic dependency tests, and the standalone
harness require explicit `--allow-server-start`. This option is not authorization
to invoke them in a task that prohibits starting a server.

The approved dump I/O adaptation adds `forgecompat.dump.path` and
`forgecompat.dump.profileDir`, keeping the original Windows defaults.
The Unix harness points both properties into each fresh run's `dumps/` directory.
Transformation/module/loading rules and `dump FAIL` assertions remain intact.
Startup rejects stale JARs without the new properties. Full runtime, official
`unix_args.txt`, original `config/defaultconfigs/spike000world`, and the fixed
Probe are required. The user supplied the original world and confirmed that
`defaultconfigs` is empty; these inputs are staged in `server-base`.

After a new build, explicitly authorized runs use:

```zsh
zsh tools/forgecompat.zsh --mode smoke \
  --server-input /workspace/dualforge-cloud/server-base \
  --probe /workspace/dualforge-cloud/inputs/forgeprobe.jar \
  --jar out/unix/build/<actual-run>/forgecompat-002-0l.jar \
  --allow-server-start \
  --java-network-args /workspace/dualforge-cloud/java-network.args \
  --public-key-relay
zsh tools/test-forge-dependencies.zsh --stage after \
  --server-input /workspace/dualforge-cloud/server-base \
  --probe /workspace/dualforge-cloud/inputs/forgeprobe.jar \
  --compat-jar out/unix/build/<actual-run>/forgecompat-002-0l.jar \
  --allow-server-start \
  --java-network-args /workspace/dualforge-cloud/java-network.args \
  --public-key-relay
```

Run these serially. Authlib explicitly uses `Proxy.NO_PROXY`; Java's ordinary
proxy properties alone do not route its public-key request. The optional relay
uses Authlib's supported services/session host properties. It listens only on
loopback, accepts only `/publickeys`, and fetches live bytes from the official
HTTPS endpoint through the platform proxy and trusted CA. It records status
and response digest, and shuts down with the harness. No TLS verification or
server-log assertions are disabled. This network transport difference is
recorded alongside the loopback bind and Unix argfile differences.

The port selects the official Unix **named-module** argfile, retaining the
original JVM feature switches. Its isolated server properties bind to
127.0.0.1 instead of all interfaces; that is an explicit cloud harness difference.
Windows creation-time restoration is not claimed on Unix; source inputs are
never used as dump output or as the writable running server directory.
