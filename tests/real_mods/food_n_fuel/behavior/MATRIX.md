# 007C-4C-5 planned matrix

Fixed main dd3d046, original FnF and accepted 4C-3 compat. No production changes.
All JVMs serial; native Forge behavior precedes NeoForge behavior. Separate stock
and observer cohorts, same observer/source/official install/fixed world seed within
Neo C/T. No source Item/Registry modification or pre-conversion.

Fuel priority: official placed furnaces, item-replace input/fuel slots, frozen world
with official tick steps at a faster official tick rate, acknowledged console
queries at exact world-game-time offsets. Never set BurnTime/CookTime or fabricate
output. Cases: paper(100)/2 cobble, bio(3200)/20 cobble, LOGO nonfuel, diamond invalid
recipe, empty input, empty fuel, cobble in smoker (wrong RecipeType for that input),
coal vanilla calibration. Campfire has no slotted fuel container: actual command
refusal is a separate unsupported-environment negative, not a claim that FnF
rejects otherwise valid smoker/blast-furnace fuel. Observe t=0,1,21,100,101,200,201,
3200,3201. Native results establish the expected burn/progress/output sequence;
Neo must match actual native state, not only a burn-time API return.

Tab: at ServerStarted, inspect cached icon/display state before querying, call
public getIconItem/buildContents using real server registry access/default flags,
query returned icon and full display/search IDs/identity/duplicates. Do not invoke
private FnF lambda or create an expected callback output. Early-cache observation
and official method bytecode establish the bounded lazy-call proof; no global
claim of uninstrumented lambda invocation counts.

Food: query every actual FnF registered Item's FOOD component, max stack, nutrition,
saturation, always-eat/eat duration/conversion, each effect ID/duration/amplifier/
probability and flags. Compare all actual food records to native, recheck carrot
bread. No invented common food effect. Real player consumption NOT RUN without a
real player lifecycle; no FoodData mutation or direct effect application.

Observer query failure is recorded as a separate gate failure with full Throwable
rather than terminating measurement of unrelated gates. Successful host boot does
not override a behavior failure. If a real difference appears, repeat the smallest
experiment with unchanged source JARs and preserve all failed attempts. No repair.
Client UI and restart/item persistence are separate NOT RUN gates. Raw evidence
stays local until authorization for this new ZIP.
