# 009C measured scopes

| Check | Evidence kind | Final-JAR status |
|---|---|---|
| Original release identity/21 classes/859 instruction references | Read-only original input | PASS |
| Baseline UNSUPPORTED_ENTRY | Actual controlled selector test, no client | PASS expected refusal |
| Finite profile and strict refusals | Final-JAR bytecode contract | PASS 101 assertions / 52 refusal checks |
| Metadata/scan/targets/foreign ownership | Actual FML APIs, modeled version/dist | PASS client15/server7 |
| Native Forge four-event API | Official bus/plugin, constructor isolation | PASS 53 assertions |
| Neo mapped events and native factory/core | Official bus/extension, diagnostic accessor | PASS 65 assertions |
| Original business members/Mixin selectors/JVM no-init | Static links and JVM verifier | PASS 728/6/21 |
| Original initialization/config Screen constructor | Injected constructor-free Minecraft model; no GUI | PASS 8 assertions |
| FnF/Clumps related contracts/server behavior/mixed install | 18 fresh worlds, final JAR | PASS listed scope |
| MOUSE_CLIENT_BOOT | Actual Windows/PCL | NOT_RUN |
| MOUSE_GUI_BEHAVIOR | Actual mouse input | NOT_RUN |
| MOUSE_CONFIG_GUI | Actual settings UI/save/reload | NOT_RUN |
| MOUSE_CLIENT_MANUAL | User A/B and mixed feedback | NOT_RUN |

009A/009B and the user's previous FnF/Clumps acceptance are historical
USER_REPORTED_PASS for their old artifact. They do not become this JAR's client
PASS. API isolation does not exercise real GUI item movement, rendering, GLFW,
Mixin application, or the public constructor bridge in a live GAME layer.

Final JAR SHA-256: `50b6ca25eecf2d08505b534df16a4af1fedf23816ca4ceb41cac09a509637db4`.
Source binding: `8f1e9008b1ca83d024ab6ad1b06a9690ac3647f6`, 31 production files.
Overall PARTIAL; measured automated scope PASS. Assessment 2008 includes per-file
checks, not 2008 independent gameplay behaviors. New-JAR real players and all
historical client/game matrices outside the listed scope remain NOT_RUN.
