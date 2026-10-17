# 010A Windows/PCL checklist — currently BLOCKED

**Do not begin compatibility acceptance with this research baseline.** The actual
NeoForge preplan still refuses JEI, and no010A client-kit ZIP is delivered.
All client business gates are NOT_RUN. The following A/B checklist is retained
for a later artifact after the required ABI contracts are implemented and tested.
The user does not need to run Ubuntu commands or investigate the current failure.

| Condition | A native baseline | B future compatible build |
|---|---|---|
| Minecraft / Java |1.21.1 /21|1.21.1 /21|
| Loader |Forge52.1.0|NeoForge21.1.256|
| Mods |original JEI Forge19.27.0.340|same original Forge JEI + accepted DualForge|

Use separate new instances/worlds, first A then B. The required original file is
`jei-1.21.1-forge-19.27.0.340.jar`, SHA-256
`f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5`.
Do not substitute the official NeoForge release, modify metadata or replace host
files. The future build must need no business JVM parameters, Agent or conversion.

1. Launch and enter a new world; verify the JEI item list.
2. Test R recipes and U uses against the same native version's actual results.
   An item with no recipe is not expected to gain one.
3. Test search filters, bookmarks/favorites, recipe categories and page switching.
4. Check shortcuts, container behavior and configuration access.
5. Change configuration, save/exit/restart and verify persistence.
6. After single-mod acceptance, add original Mouse Tweaks, FnF and Clumps. Check
   mouse input conflicts, FnF item visibility/search and actual recipes, JEI GUI,
   food/fuel, XP/Mending and save/exit/re-entry.

Record A/B PASS/FAIL/NOT_RUN per step and the actual mods SHA. Preserve latest.log,
debug.log, first crash if any, configuration changes and relevant screenshots or
short videos. No account details or private worlds are needed. Existing009C user
PASS cannot substitute for this future artifact's actual client test.
