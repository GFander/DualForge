package io.github.gfander.dualforge.metadata;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.api.*;
import io.github.gfander.dualforge.ForgeModReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.locating.*;
import org.objectweb.asm.tree.ClassNode;

/** Test-only observer. Delegates file creation to production and records FML's assigned loader. */
public final class RuntimeSelectionProbe implements IModFileReader, ITransformationService {
    private static final List<IModFile> files = new ArrayList<>();
    @Override public int getPriority() { return 200; }
    @Override public IModFile read(JarContents contents, ModFileDiscoveryAttributes attributes) {
        if (contents.findFile("META-INF/mods.toml").isEmpty()) return null;
        IModFile file = new ForgeModReader().read(contents, attributes);
        if (file != null) files.add(file);
        return file;
    }
    @Override public String name() { return "metadata-selection-observer"; }
    @Override public void initialize(IEnvironment environment) { }
    @Override public void onLoad(IEnvironment environment, Set<String> others) { }
    @Override public List<? extends ITransformer<?>> transformers() {
        return List.of(new ITransformer<ClassNode>() {
            @Override public TargetType<ClassNode> getTargetType() { return TargetType.CLASS; }
            @Override public ClassNode transform(ClassNode node, ITransformerVotingContext context) { return node; }
            @Override public TransformerVoteResult castVote(ITransformerVotingContext context) { return TransformerVoteResult.NO; }
            @Override public Set<Target<ClassNode>> targets() {
                FMLLoader.getLanguageLoadingProvider().forEach(provider -> System.out.println(
                        "[DF007C1-PROVIDER] enumerated=" + provider.getClass().getName() + " version=" + provider.version()));
                for (IModFile file : files) {
                    for (var loader : file.getLoaders()) System.out.println("[DF007C1-PROVIDER] assigned file="
                            + file.getFileName() + " ids=" + file.getModInfos().stream().map(i -> i.getModId()).toList()
                            + " ranges=" + file.getModFileInfo().requiredLanguageLoaders()
                            + " loader=" + loader.getClass().getName() + " version=" + loader.version());
                }
                return Set.of(); // No targets, mutation, dependency overrides or synthetic mod.
            }
        });
    }
}
