"""Capture exact source static initialization, queue/connection ABI and codec host binaries."""
import argparse
from pathlib import Path
import sys
import zipfile
import hashlib
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,run,record,save

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
    jei=ENV/'runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar'
    assert hashlib.sha256(jei.read_bytes()).hexdigest()=='f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5'
    for cls in ['mezz.jei.common.Internal','mezz.jei.common.JeiFeatures','mezz.jei.api.runtime.IJeiFeatures','mezz.jei.common.config.GiveMode','mezz.jei.common.transfer.TransferOperation']:
        # TransferOperation's exact path comes from the source atlas; no guessed class can pass this collector.
        if cls.endswith('TransferOperation'):
            with zipfile.ZipFile(jei) as z:matches=[n for n in z.namelist() if n.endswith('/TransferOperation.class')];assert len(matches)==1;cls=matches[0][:-6].replace('/','.')
        run(out,'source-'+cls,[JDK/'bin/javap','-p','-c','-s','-v','-classpath',jei,cls])
    names=['net.minecraft.util.thread.BlockableEventLoop','net.minecraft.util.thread.ReentrantBlockableEventLoop','net.minecraft.network.Connection','net.minecraft.network.ConnectionProtocol','net.minecraft.network.protocol.common.ServerCommonPacketListener','net.minecraft.network.codec.StreamCodec','net.minecraft.network.codec.ByteBufCodecs']
    selected=[]
    for side in ['forge','neo']:
        deps=sorted((ENV/('runs/convergence-009c-20261017/client-abi/'+side+'/libraries')).rglob('*.jar'),key=lambda p:(not p.name.endswith('-server.jar'),str(p)))
        for cls in names:
            member=cls.replace('.','/')+'.class';chosen=None
            for jar in deps:
                with zipfile.ZipFile(jar) as z:
                    if member in z.namelist():chosen={'side':side,'class':cls,'jar':record(jar),'classSHA256':hashlib.sha256(z.read(member)).hexdigest()};break
            assert chosen,cls;selected.append(chosen);run(out,side+'-'+cls,[JDK/'bin/javap','-p','-c','-s','-classpath',':'.join(map(str,deps)),cls])
    save(out/'binary-bindings.json',selected);save(out/'result.json',{'status':'PASS','scope':'STATIC_SOURCE_HOST_ABI','jei':record(jei)})
if __name__=='__main__':main()
