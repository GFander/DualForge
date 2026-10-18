# Pinned architecture references

Read-only clones and command/commit receipts live in the task SSD `references/`.
No code is copied; none is a dependency, input mod or compatibility oracle.

| Reference | Exact revision | Observation | DualForge decision |
|---|---|---|---|
| [Kilt NeoForgeMod](https://github.com/KiltMC/Kilt/blob/8f008c5b427be2aedd87419a9bf7b99d8271395e/src/main/kotlin/xyz/bluspring/kilt/loader/mod/NeoForgeMod.kt) | version/1.21.1, 8f008c5 | Existing container bus first; constrained lazy fallback per mod | Reuse the Neo official bus objects; no fallback/shadow bus |
| [Kilt EventBusesHooks](https://github.com/KiltMC/Kilt/blob/8f008c5b427be2aedd87419a9bf7b99d8271395e/src/main/kotlin/dev/architectury/platform/hooks/EventBusesHooks.kt) | same revision | Mod ID lookup delivers that mod's bus | Mod ID is not sufficient authorization; additionally bind actual container/file/module/loader/layer |
| [FFA lifecycle adapter](https://github.com/Sinytra/ForgifiedFabricAPI/blob/204211bf9c26bcb87379cefbca4ba10e75adee15/fabric-lifecycle-events-v1/src/main/java/net/fabricmc/fabric/impl/event/lifecycle/LifecycleEventsImpl.java) | 1.21.1, 204211b | Real Neo listeners invoke Fabric lifecycle callbacks | Separate event semantics from provenance; avoid an unnecessary forwarding Consumer for equivalent native events |
| [FFA interaction adapter](https://github.com/Sinytra/ForgifiedFabricAPI/blob/204211bf9c26bcb87379cefbca4ba10e75adee15/fabric-events-interaction-v0/src/main/java/net/fabricmc/fabric/impl/event/interaction/InteractionEventHooks.java) | same revision | Cancellation, action outcomes and parameters explicitly translated | Only validate finite cancellable LivingDeath contract; arbitrary result mappings refuse |
| [FFA EventFactoryImpl](https://github.com/Sinytra/ForgifiedFabricAPI/blob/204211bf9c26bcb87379cefbca4ba10e75adee15/fabric-api-base/src/main/java/net/fabricmc/fabric/impl/base/event/EventFactoryImpl.java) | same revision | Callback invoker construction and weak event registry | Do not import Fabric's callback machinery; weak cache values cannot retain live owners |
| [Architectury Forge EventBuses](https://github.com/architectury/architectury-api/blob/321ad305a1caacd20ea655f4c392a87c3376a244/minecraftforge/src/main/java/dev/architectury/platform/forge/EventBuses.java) | 1.21, 321ad30 | Per-mod bus registry rejects duplicate registration and delivers pending callbacks | Reject duplicates; no bus merging or late unproven source registration |
| [Architectury Neo hooks](https://github.com/architectury/architectury-api/blob/321ad305a1caacd20ea655f4c392a87c3376a244/neoforge/src/main/java/dev/architectury/platform/hooks/EventBusesHooks.java) | same revision | ModList container -> getEventBus | Resolve in actual host infrastructure, with stronger verified owner provenance |

These projects have different hosts and API goals. Their behavior does not prove
Forge/Neo event, configuration or module equivalence for DualForge. The fixed
platform ABI, independent original Forge JARs, native controls and real FML
runs provide that separate evidence. Custom bus support is not implemented.
