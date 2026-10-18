package io.github.gfander.dualforge;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;

/** Candidate routing only. A selected profile must still pass its original validator. */
final class ForgeEntryProfile {
    enum Kind { BLOCK, NORMAL_ITEM, LEGACY_ITEM, EVENT, CONTEXT, EVENT_INITIALIZER, CLIENT_EVENTS }

    static Kind select(ClassNode node, boolean ownedEvent) {
        List<Kind> candidates = new ArrayList<>();
        boolean block = ControlledForgeBlockProfile.isCandidate(node);
        boolean itemFields = node.fields.stream().anyMatch(f -> f.desc.equals("Lnet/minecraftforge/registries/DeferredRegister;")
                || f.desc.equals("Lnet/minecraftforge/registries/RegistryObject;"));
        boolean normal = !block && itemFields && ControlledForgeItemProfile.isNormalCandidate(node);
        if (block) candidates.add(Kind.BLOCK);
        else if (normal) candidates.add(Kind.NORMAL_ITEM);
        else if (itemFields) candidates.add(Kind.LEGACY_ITEM);
        if (ownedEvent) candidates.add(Kind.EVENT);
        if (ControlledForgeContextProfile.isCandidate(node)) candidates.add(Kind.CONTEXT);
        if (ForgeEventInitializerPlan.isCandidate(node)) candidates.add(Kind.EVENT_INITIALIZER);
        if (ForgeClientEventPlan.isCandidate(node)) candidates.add(Kind.CLIENT_EVENTS);
        if (candidates.isEmpty()) throw new IllegalStateException("UNSUPPORTED_ENTRY " + node.name + ": no controlled Forge entry profile");
        if (candidates.size() != 1) throw new IllegalStateException("AMBIGUOUS_ENTRY " + node.name + ": " + candidates);
        return candidates.getFirst();
    }
}
