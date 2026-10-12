# Deprecated compatibility snapshot and Gradle record

This directory's `src/` and `archive/` contain legacy discovery-only snapshots.
They are preserved at their original locations and are not production sources.

The old Gradle file was moved without changing its bytes to
[history/compat-build.gradle.deprecated](../../history/compat-build.gradle.deprecated).
Gradle is not a supported build entrypoint. Use the zsh/Python toolchain:

```zsh
zsh tools/entry/forgecompat.zsh --mode build --server-input /absolute/server-base
```

Run from the checkout root. The only production sources are
`spike001a/compat-002-0l/src/main/java` and `src/main/resources`.
See [STRUCTURE.md](../../STRUCTURE.md) for active tools and archived versions.
