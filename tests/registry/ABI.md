# Convergence-005 Gate 0 / Gate 2

Base: `20e69fae7a07c6a5b74f017c29762f414834814c` (merged PR #6).
Locked Forge 52.1.0 / bus 6.2.27, NeoForge 21.1.256, Minecraft 1.21.1,
FML 4.0.45, ModLauncher 11.0.5. Actual commands, tool versions and hashes
are recorded under `out/convergence-005/contract/` and `baseline-build/`.

## Observed ABI

Descriptors below were read with the installed JDK's `javap -p -s -c`.
`F` means `net/minecraftforge/registries/`, `N` means
`net/neoforged/neoforge/registries/`. These are owner abbreviations only.

| Operation | Forge descriptor | NeoForge descriptor |
| --- | --- | --- |
| Registry field | `ForgeRegistries.ITEMS:LF/IForgeRegistry;` | `BuiltInRegistries.ITEM:Lnet/minecraft/core/DefaultedRegistry;` |
| DeferredRegister.create | `(LF/IForgeRegistry;Ljava/lang/String;)LF/DeferredRegister;` | `(Lnet/minecraft/core/Registry;Ljava/lang/String;)LN/DeferredRegister;` |
| register item supplier | `(Ljava/lang/String;Ljava/util/function/Supplier;)LF/RegistryObject;` | `(Ljava/lang/String;Ljava/util/function/Supplier;)LN/DeferredHolder;` |
| attach MOD bus | `(Lnet/minecraftforge/eventbus/api/IEventBus;)V` | `(Lnet/neoforged/bus/api/IEventBus;)V` |
| holder.get | `RegistryObject.get:()Ljava/lang/Object;` | `DeferredHolder.get:()Ljava/lang/Object;` |
| Item constructor (both) | `(Lnet/minecraft/world/item/Item$Properties;)V` | same |
| Item.Properties constructor (both) | `()V` | same |

Both holders erase `get()` to Object, but their classes and the return types
of `register(String,Supplier)` differ. The input demands RegistryObject in
its static field, PUTSTATIC, GETSTATIC and method owner. Converting only the
register call would leave those JVM type requirements unsatisfied. A cast
cannot establish holder identity. The bounded mapping must change all those
references consistently, preserving the caller's existing cast to Item.

The native Forge compiler emits a zero-capture Supplier `invokedynamic`:
`LambdaMetafactory.metafactory`, erased `()Object`, H_INVOKESTATIC to the
same entry's private synthetic `lambda$static$0()Item`, instantiated `()Item`.
Its bootstrap types contain vanilla Item, not Forge APIs. No bootstrap
rewrite is required for this exact shape; unknown bootstrap profiles must
be rejected. The static initializer creates one DeferredRegister and one
supplier/holder. The constructor has exactly the original single Forge
IEventBus argument.

## Lifecycle evidence and design scope

FMLModContainer creates its own bus with `markerType(IModBusEvent.class)`;
`constructMod` maps NeoForge IEventBus to that container's eventBus, and
`getEventBus()` returns it. This differs from `NeoForge.EVENT_BUS`.
CommonModLoader's begin sequence calls postNewRegistryEvent, unfreezeData,
GameData.postRegisterEvents, freezeData. GameData constructs RegisterEvent
and calls ModLoader.postEventWrapContainerInModOrder. DeferredRegister's
listener matches the registry key, invokes RegisterEvent.register with the
stored supplier, and binds the DeferredHolder. This is static bytecode
evidence until the real C/T stack and registry queries are run.

Forge DeferredRegister/RegistryObject are compiler inputs, not installed
runtime libraries. A successful GAME-layer load must resolve the mapped
NeoForge owners; the existing empty Forge IEventBus shell is not a registry
implementation. Only the scan-owned entry may be converted. Existing
subscriber/Event profiles and their constraints must remain intact.

The exact bounded profile includes two final static Item-typed fields,
one constructor, one get-only accessor, one zero-capture supplier (marker
then ordinary Item construction), and one single-entry static initializer.
It permits the controlled property branch and inert print/identity markers.
Field/method/entry names and modId are derived, not identity whitelists.
No Block, custom registry, arbitrary supplier, extra registration, RegistryObject
API, constructor ABI, or general remapping is promised.

## Actual baseline observation

The clean 004 compat is SHA-256
`3e4aa3865cd20df968cce7e401d3fe8624d18b86cb829a88d2467b781e716790`.
Run `out/convergence-005/baseline/20261012-165100-fef8857b/` confirms one
Locator discovery, Reader handoff and real `{registryprobe}` metadata, then:

```text
java.lang.IllegalStateException: 0l expected one same-file event profile for com.example.registryprobe.RegistryProbe
  at ...ForgeProbeConstructorTransformer.targets(ForgeProbeConstructorTransformer.java:332)
```

The exception is in raw stderr, before construction, GAME-layer registration
and Done. The earlier run `20261012-165006-1875ef40` has the same real failure,
but its runner incorrectly searched stdout only and is recorded FAIL; it was
not overwritten. Baseline assertion PASS means the rejection was reproduced,
not that Registry worked. Both outputs were frozen before production edits.

Native compilation uses the installed remapped *pre-patch vanilla* Minecraft
jar plus pinned Forge and vanilla dependencies. Every Minecraft class in
that input was checked for Forge/NeoForge owner references; none were found.
The fixture compile classpath and class output contain no NeoForge or compat.
The probe currently lacks the user's original PNG file. Java compilation
PASS does not mean complete resource Gate 1 PASS or client rendering PASS.
