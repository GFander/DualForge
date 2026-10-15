package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import java.nio.file.*;
import java.util.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.*;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.tree.*;

/** Read-only Gate 8. A precise refusal is not a successful FnF runtime. */
public final class FnFRegistryAdmission {
    public static void main(String[] args)throws Exception {
        Path jar=Path.of(args[0]);byte[] before=Files.readAllBytes(jar);
        if(!ForgeRegistryTransformationPlan.digest(before).equals("bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958"))throw new AssertionError("Unexpected FnF input");
        var v=FMLLoader.class.getDeclaredField("versionInfo");v.setAccessible(true);v.set(null,new VersionInfo("21.1.256","4.0.45","1.21.1","20240808.144430"));
        var d=FMLLoader.class.getDeclaredField("dist");d.setAccessible(true);d.set(null,Dist.DEDICATED_SERVER);
        ModFile file=(ModFile)new ForgeModReader().read(JarContents.of(jar),ModFileDiscoveryAttributes.DEFAULT);
        file.setScanResult(file.compileContent(),null);LoadingModList.of(List.of(),List.of(file),List.of(),List.of(),Map.of());
        ClassNode entry=ForgeRegistryTransformationPlan.read(Files.readAllBytes(file.findResource("net/natevg/fnf/FoodNFuel.class")));
        if(ForgeEntryProfile.select(entry,false)!=ForgeEntryProfile.Kind.CONTEXT || ForgeRegistryTransformationPlan.isCandidate(entry))throw new AssertionError("Old global-bus Context boundary changed");
        System.out.println("FnF old Context retained; new Helper candidate excluded by unverified global bus");
        if(args.length>1) {
            var oracle=new PinnedHostOracle(Path.of(args[1]));
            boolean typed=args.length>2 && args[2].equals("typed-v1");
            Runnable admission=() -> ForgeRegistryTransformationPlan.create(file.getModInfos().getFirst(),entry.name,oracle);
            if(typed)FnFAdmissionExpectation.requireTypedV1Rejection(admission);else FnFAdmissionExpectation.requireSupplierV1Rejection(admission);
            System.out.println("FNF_CONTRACT_VERSION="+(typed?"typed-v1":"supplier-v1"));
            System.out.println("FNF_PLAN_ADMISSION=REJECTED reason="+(typed?FnFAdmissionExpectation.TYPED_V1_REASON:FnFAdmissionExpectation.SUPPLIER_V1_REASON));
        } else {
            FnFAdmissionExpectation.requireSupplierRejection(() -> ForgeRegistryTransformationPlan.create(file.getModInfos().getFirst(),entry.name));
            System.out.println("FNF_PLAN_ADMISSION=REJECTED reason="+FnFAdmissionExpectation.REASON);
        }
        for(String name:List.of("net/natevg/fnf/item/ItemRegistries","net/natevg/fnf/item/FnFCreativeModeTabs")) {
            ClassNode n=ForgeRegistryTransformationPlan.read(Files.readAllBytes(file.findResource(name+".class")));int count=0;
            for(FieldNode f:n.fields)System.out.println("FIELD "+name+" "+f.name+" "+f.desc+" signature="+f.signature);
            for(MethodNode m:n.methods)for(var ins:m.instructions)if(ins instanceof InvokeDynamicInsnNode i) {
                count++;System.out.println("INDY "+name+" "+m.name+m.desc+" "+i.name+i.desc+" bsm="+i.bsm+" args="+Arrays.toString(i.bsmArgs));
            }
            System.out.println("INDY_COUNT "+name+"="+count);
        }
        if(!Arrays.equals(before,Files.readAllBytes(jar)))throw new AssertionError("FnF modified");
        System.out.println("FNF READ_ONLY ADMISSION PASS; FNF_RUNTIME=NOT RUN; no helper output published");
    }
}
