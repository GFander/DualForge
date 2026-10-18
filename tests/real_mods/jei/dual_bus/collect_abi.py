"""Revalidate original JEI bytes, retain instruction graph and fresh pinned-JDK javap evidence."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import sys
import zipfile
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save,sha

FORGE_SHA='f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5'
NEO_SHA='8aaf547432f1b4958239b036356b910692fe40f858c3073d996f56bbf7c99826'

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
    previous=ENV/'runs/convergence-010a-jei';inputs=previous/'inputs';scan=previous/'static/usage.jsonl.gz'
    originals={'forge-jei':inputs/'jei-1.21.1-forge-19.27.0.340.jar','neo-jei':inputs/'jei-1.21.1-neoforge-19.27.0.340.jar'}
    assert sha(originals['forge-jei'])==FORGE_SHA and sha(originals['neo-jei'])==NEO_SHA
    jars={key:zipfile.ZipFile(path) for key,path in originals.items()};rows=[];counts={key:0 for key in jars}
    with gzip.open(scan,'rt') as f:
        for line in f:
            row=json.loads(line);key=row['artifact'];assert key in jars
            assert hashlib.sha256(jars[key].read(row['member'])).hexdigest()==row['classSHA256'];counts[key]+=1
            if key=='forge-jei':rows.append(row)
    assert counts=={'forge-jei':923,'neo-jei':923}
    graph=[];event_types=set()
    for row in rows:
        for method in row['code']:
            instructions=method['instructions']
            for at,op in enumerate(instructions):
                owner=op.get('owner','')
                relevant=owner.startswith('net/minecraftforge/') and any(s in owner for s in ['eventbus/','fml/','ForgeConfigSpec','MinecraftForge'])
                relevant|=owner.startswith('mezz/jei/forge/events/') and op.get('name') in ['<init>','register','unregister','getModEventBus']
                if relevant:
                    nearby=instructions[max(0,at-10):at+3]
                    for item in nearby:
                        typ=item.get('constant',{});typ=typ.get('type','') if isinstance(typ,dict) else ''
                        if typ.startswith('Lnet/minecraftforge/') and '/event/' in typ:event_types.add(typ)
                    graph.append({'sourceClass':row['name'],'classSHA256':row['classSHA256'],'sourceMethod':method['name'],'declaredDescriptor':method['descriptor'],
                        'instruction':op,'nearbyInstructions':nearby,'owner':'JEI ModFile; ownership/runtime reachability remains unapproved','evidenceKind':'VERIFIED_ORIGINAL_CLASSFILE_INSTRUCTIONS','lifecycle':'constructor' if method['name']=='<init>' else 'method-specific; see raw code',
                        'expectedHostMapping':'NOT_APPROVED except independently tested finite fixture contracts',
                        'actualBusOrigin':'GAME field' if owner.endswith('/MinecraftForge') else 'MOD_OWNED getter' if op.get('name')=='getModEventBus' else 'helper-dependent; original method instructions retained',
                        'consumerBootstrapCandidates':[item for item in nearby if item.get('opcode')==186],
                        'unverifiedConditions':['actual host event identity','lifecycle','same-JAR closure authorization','consumer target and capture if not in nearby window']})
    save(out/'bus-abi.json',{'status':'STATIC_VERIFIED_ONLY','inputs':[record(p) for p in originals.values()],'sourceScan':record(scan),'classSHARevalidated':counts,
        'coordinateSystem':'ASM instruction indexes; fresh javap logs provide byte offsets/bootstrap tables','observedEventClassConstants':sorted(event_types),'callSites':graph,'registrationMethods':[{'sourceClass':row['name'],'classSHA256':row['classSHA256'],'method':m} for row in rows for m in row['code'] if any(op.get('owner','').startswith('mezz/jei/forge/events/') and op.get('name')=='register' for op in m['instructions'])]})
    wanted=['mezz.jei.forge.JustEnoughItems','mezz.jei.forge.events.PermanentEventSubscriptions','mezz.jei.forge.events.RuntimeEventSubscriptions','mezz.jei.forge.events.EventSubscription','mezz.jei.forge.config.ServerConfig','mezz.jei.forge.network.NetworkHandler','mezz.jei.forge.JustEnoughItemsClientSafeRunner','mezz.jei.forge.JustEnoughItemsClient','mezz.jei.forge.startup.EventRegistration','mezz.jei.forge.startup.StartEventObserver','mezz.jei.core.util.WeakConsumer']
    for name in wanted:run(out,'javap-'+name,[JDK/'bin/javap','-p','-c','-s','-v','-classpath',originals['forge-jei'],name])
    for side in ['forge','neo']:
        libs=sorted((ENV/('runs/convergence-009c-20261017/client-abi/'+side+'/libraries')).rglob('*.jar'))
        types=['net.minecraftforge.eventbus.api.IEventBus','net.minecraftforge.common.ForgeConfigSpec$Builder','net.minecraftforge.fml.config.IConfigSpec','net.minecraftforge.fml.ModContainer'] if side=='forge' else ['net.neoforged.bus.api.IEventBus','net.neoforged.neoforge.common.ModConfigSpec$Builder','net.neoforged.fml.config.IConfigSpec','net.neoforged.fml.ModContainer','net.neoforged.fml.config.ConfigTracker']
        save(out/(side+'-sdk-inputs.json'),[record(p) for p in libs])
        run(out,side+'-host-javap',[JDK/'bin/javap','-p','-c','-s','-classpath',':'.join(map(str,libs)),*types])
    save(out/'result.json',{'status':'PASS','scope':'revalidated bytecode research; not live JEI compatibility','inputs':[record(p) for p in originals.values()],'sites':len(graph),'classes':counts,'sourceScan':record(scan)})
    print('JEI ORIGINAL ABI REVALIDATED sites='+str(len(graph)))
if __name__=='__main__':main()
