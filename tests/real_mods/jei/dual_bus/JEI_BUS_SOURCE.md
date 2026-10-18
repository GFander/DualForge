# Original JEI bus/config source graph (010B)

This is original-ClassFile research, not approval of a production JEI closure.
Both original release SHAs and all 923+923 class SHAs are revalidated. Fresh
Microsoft JDK21 javap code/descriptors/bootstrap tables and full registration
method instructions are in SSD `jei-abi-final/`; the 010B evidence index binds
those files. Coordinates below are **ASM instruction indexes**, unless explicitly
called byte offsets. No original JAR is changed.

```text
JustEnoughItems.<init>()V
  byte 4: NEW MinecraftLocaleSupplier -> Translator.setLocaleSupplier
  byte 14: MinecraftForge.EVENT_BUS -> local 1 (GAME)
  byte 18/21: FMLJavaModLoadingContext.get/getModEventBus -> local 2 (MOD_OWNED)
  byte 31: PermanentEventSubscriptions(local 1, local 2)
      -> final forgeEventBus/modEventBus fields, same argument objects
      -> register(Class, Consumer): IModBusEvent.isAssignableFrom(Class)
          true: modEventBus.addListener(NORMAL,false,Class,Consumer)
          false: forgeEventBus.addListener(NORMAL,false,Class,Consumer)
  byte 35/42: ModLoadingContext.get -> ServerConfig.register(context)
      Builder -> ServerConfig(Builder) -> build -> Type.SERVER -> registerConfig
  byte 61: NetworkHandler(serverConfig, ResourceLocation("jei","channel"), 3)
  byte 73: ClientSafeRunner(networkHandler, subscriptions)
  byte 83/88: captured Supplier -> DistExecutor.safeRunWhenOn(CLIENT,...)
```

Exact getter descriptors are
`()Lnet/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext;` and
`()Lnet/minecraftforge/eventbus/api/IEventBus;`.
The two-bus constructor is
`(Lnet/minecraftforge/eventbus/api/IEventBus;Lnet/minecraftforge/eventbus/api/IEventBus;)V`.
Both explicit listener sites use
`(Lnet/minecraftforge/eventbus/api/EventPriority;ZLjava/lang/Class;Ljava/util/function/Consumer;)V`.
Source IEventBus.post returns boolean; Neo returns Event. Source post is not
admitted by this new profile. Domain cannot be inferred merely from class names.

RuntimeEventSubscriptions is different: it stores a supplied Game Bus, refuses
IModBusEvent registration, retains EventSubscription objects in a List and
unregisters/clears them. EventSubscription wraps the **original** business
Consumer in JEI's WeakConsumer, registers that wrapper at NORMAL/false, retains
both fields and unregisters its registeredListener wrapper. This original
wrapper/lifetime policy is not equivalent to a direct strong Consumer. The new
finite constructor profile does not claim it.

ServerConfig uses real ForgeConfigSpec.Builder, three boolean definitions with
defaults true/true/false and Supplier<Boolean> getters. The real factory returns
IServerConfig; it is not the new finite fixture's concrete return shape.
Forge IConfigSpec and native IConfigSpec differ structurally; no direct cast is
justified. CLIENT-only config loading has not been run.

The full entry is rejected **before any JEI target is published or constructed**.
The current guard expects immediate Game Bus acquisition after Object.<init>;
the real first operation is the locale constructor at byte 4. Its current error
is UNKNOWN_BUS_ORIGIN (field acquisition contract mismatch). This does not mean
that the original official Game Bus origin is unknown in this static graph, or
that JEI bus/config runtime already passed. Network/Dist/client callbacks are
subsequent unexecuted requirements, not a claimed new runtime failure.

The following exact Class/Consumer producers are original bytecode observations.
All owner fields refer to JEI's original ModFile/module; mapping and lifecycle
on NeoForge remain **UNVERIFIED**. Captures are the invokedynamic descriptors;
H5/H6 refer to invokevirtual/invokestatic method handles. The original bootstrap
is LambdaMetafactory for these sites. The Set-driven StartEventObserver case
has a dynamic Class parameter, with source class constants recorded separately.

| Source class/method | ASM register index | Class argument | Consumer target | Capture descriptor |
|---|---|---|---|---|
| `mezz/jei/forge/JustEnoughItemsClient.register` | `7` | `Lnet/minecraftforge/client/event/RegisterClientReloadListenersEvent;` | `H5 mezz/jei/forge/JustEnoughItemsClient.onRegisterReloadListenerEvent(Lnet/minecraftforge/client/event/RegisterClientReloadListenersEvent;)V` | `(Lmezz/jei/forge/JustEnoughItemsClient;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/JustEnoughItemsClient.register` | `15` | `Lnet/minecraftforge/client/event/RegisterClientTooltipComponentFactoriesEvent;` | `H5 mezz/jei/forge/JustEnoughItemsClient.onRegisterClientTooltipEvent(Lnet/minecraftforge/client/event/RegisterClientTooltipComponentFactoriesEvent;)V` | `(Lmezz/jei/forge/JustEnoughItemsClient;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/JustEnoughItemsClient.register` | `22` | `Lnet/minecraftforge/client/event/RegisterKeyMappingsEvent;` | `H6 mezz/jei/forge/JustEnoughItemsClient.lambda$register$0(Lnet/minecraftforge/client/event/RegisterKeyMappingsEvent;)V` | `()Ljava/util/function/Consumer;` |
| `mezz/jei/forge/events/RuntimeEventSubscriptions.register` | `27` | `Class parameter (Game-only guard)` | `original Consumer -> EventSubscription/WeakConsumer` | `parameter; see original descriptor (Ljava/lang/Class;Ljava/util/function/Consumer;)V` |
| `mezz/jei/forge/startup/EventRegistration.registerClientInputHandler` | `6` | `Lnet/minecraftforge/client/event/ScreenEvent$Init$Post;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerClientInputHandler$0(Lmezz/jei/gui/input/ClientInputHandler;Lnet/minecraftforge/client/event/ScreenEvent$Init$Post;)V` | `(Lmezz/jei/gui/input/ClientInputHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerClientInputHandler` | `13` | `Lnet/minecraftforge/client/event/ScreenEvent$KeyPressed$Pre;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerClientInputHandler$1(Lmezz/jei/gui/input/ClientInputHandler;Lnet/minecraftforge/client/event/ScreenEvent$KeyPressed$Pre;)V` | `(Lmezz/jei/gui/input/ClientInputHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerClientInputHandler` | `20` | `Lnet/minecraftforge/client/event/ScreenEvent$KeyPressed$Post;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerClientInputHandler$2(Lmezz/jei/gui/input/ClientInputHandler;Lnet/minecraftforge/client/event/ScreenEvent$KeyPressed$Post;)V` | `(Lmezz/jei/gui/input/ClientInputHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerClientInputHandler` | `27` | `Lnet/minecraftforge/client/event/ScreenEvent$CharacterTyped$Pre;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerClientInputHandler$3(Lmezz/jei/gui/input/ClientInputHandler;Lnet/minecraftforge/client/event/ScreenEvent$CharacterTyped$Pre;)V` | `(Lmezz/jei/gui/input/ClientInputHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerClientInputHandler` | `34` | `Lnet/minecraftforge/client/event/ScreenEvent$CharacterTyped$Post;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerClientInputHandler$4(Lmezz/jei/gui/input/ClientInputHandler;Lnet/minecraftforge/client/event/ScreenEvent$CharacterTyped$Post;)V` | `(Lmezz/jei/gui/input/ClientInputHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerClientInputHandler` | `41` | `Lnet/minecraftforge/client/event/ScreenEvent$MouseButtonPressed$Pre;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerClientInputHandler$6(Lmezz/jei/gui/input/ClientInputHandler;Lnet/minecraftforge/client/event/ScreenEvent$MouseButtonPressed$Pre;)V` | `(Lmezz/jei/gui/input/ClientInputHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerClientInputHandler` | `48` | `Lnet/minecraftforge/client/event/ScreenEvent$MouseButtonReleased$Pre;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerClientInputHandler$8(Lmezz/jei/gui/input/ClientInputHandler;Lnet/minecraftforge/client/event/ScreenEvent$MouseButtonReleased$Pre;)V` | `(Lmezz/jei/gui/input/ClientInputHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerClientInputHandler` | `55` | `Lnet/minecraftforge/client/event/ScreenEvent$MouseScrolled$Pre;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerClientInputHandler$9(Lmezz/jei/gui/input/ClientInputHandler;Lnet/minecraftforge/client/event/ScreenEvent$MouseScrolled$Pre;)V` | `(Lmezz/jei/gui/input/ClientInputHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerGuiHandler` | `6` | `Lnet/minecraftforge/client/event/ScreenEvent$Init$Post;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerGuiHandler$10(Lmezz/jei/gui/events/GuiEventHandler;Lnet/minecraftforge/client/event/ScreenEvent$Init$Post;)V` | `(Lmezz/jei/gui/events/GuiEventHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerGuiHandler` | `13` | `Lnet/minecraftforge/client/event/ScreenEvent$Opening;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerGuiHandler$11(Lmezz/jei/gui/events/GuiEventHandler;Lnet/minecraftforge/client/event/ScreenEvent$Opening;)V` | `(Lmezz/jei/gui/events/GuiEventHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerGuiHandler` | `20` | `Lnet/minecraftforge/client/event/ContainerScreenEvent$Render$Foreground;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerGuiHandler$12(Lmezz/jei/gui/events/GuiEventHandler;Lnet/minecraftforge/client/event/ContainerScreenEvent$Render$Foreground;)V` | `(Lmezz/jei/gui/events/GuiEventHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerGuiHandler` | `27` | `Lnet/minecraftforge/client/event/ScreenEvent$Render$Post;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerGuiHandler$13(Lmezz/jei/gui/events/GuiEventHandler;Lnet/minecraftforge/client/event/ScreenEvent$Render$Post;)V` | `(Lmezz/jei/gui/events/GuiEventHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/EventRegistration.registerGuiHandler` | `34` | `Lnet/minecraftforge/client/event/ScreenEvent$RenderInventoryMobEffects;` | `H6 mezz/jei/forge/startup/EventRegistration.lambda$registerGuiHandler$14(Lmezz/jei/gui/events/GuiEventHandler;Lnet/minecraftforge/client/event/ScreenEvent$RenderInventoryMobEffects;)V` | `(Lmezz/jei/gui/events/GuiEventHandler;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/StartEventObserver.register` | `15` | `Lnet/minecraftforge/client/event/ClientPlayerNetworkEvent$LoggingIn;` | `H5 mezz/jei/forge/startup/StartEventObserver.lambda$register$1(Lnet/minecraftforge/client/event/ClientPlayerNetworkEvent$LoggingIn;)V` | `(Lmezz/jei/forge/startup/StartEventObserver;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/StartEventObserver.register` | `22` | `Lnet/minecraftforge/client/event/ClientPlayerNetworkEvent$LoggingOut;` | `H5 mezz/jei/forge/startup/StartEventObserver.lambda$register$2(Lnet/minecraftforge/client/event/ClientPlayerNetworkEvent$LoggingOut;)V` | `(Lmezz/jei/forge/startup/StartEventObserver;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/StartEventObserver.register` | `29` | `Lnet/minecraftforge/client/event/ScreenEvent$Init$Pre;` | `H5 mezz/jei/forge/startup/StartEventObserver.lambda$register$4(Lnet/minecraftforge/client/event/ScreenEvent$Init$Pre;)V` | `(Lmezz/jei/forge/startup/StartEventObserver;)Ljava/util/function/Consumer;` |
| `mezz/jei/forge/startup/StartEventObserver.lambda$register$0` | `6` | `dynamic Class parameter` | `H5 mezz/jei/forge/startup/StartEventObserver.onEvent(Lnet/minecraftforge/eventbus/api/Event;)V` | `(Lmezz/jei/forge/startup/StartEventObserver;)Ljava/util/function/Consumer;` |

Callback body side effects, exception propagation and thread/lifetime on actual JEI Neo runtime are NOT_RUN. Finite fixture/API tests supply separate evidence only.
