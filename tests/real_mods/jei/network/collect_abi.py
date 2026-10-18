"""Revalidate both original JEI releases, retain the full source graph and exact SDK network binaries."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import sys
import zipfile
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save,sha

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
    previous=ENV/'runs/convergence-010a-jei';originals={side:previous/('inputs/jei-1.21.1-'+side+'-19.27.0.340.jar') for side in ['forge','neoforge']}
    expected=['f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5','8aaf547432f1b4958239b036356b910692fe40f858c3073d996f56bbf7c99826']
    assert [sha(p) for p in originals.values()]==expected
    archives={('forge-jei' if side=='forge' else 'neo-jei'):zipfile.ZipFile(jar) for side,jar in originals.items()};rows=[];counts={key:0 for key in archives}
    scan=previous/'static/usage.jsonl.gz'
    with gzip.open(scan,'rt') as stream:
        for line in stream:
            row=json.loads(line);assert hashlib.sha256(archives[row['artifact']].read(row['member'])).hexdigest()==row['classSHA256'];counts[row['artifact']]+=1;rows.append(row)
    assert counts=={'forge-jei':923,'neo-jei':923}
    with gzip.open(out/'complete-source-graph.jsonl.gz','wt') as stream:
        for row in rows:stream.write(json.dumps(row,ensure_ascii=False)+'\n')
    selected=[row for row in rows if '/network/' in row['name'] or row['name'] in ['mezz/jei/common/Internal','mezz/jei/forge/JustEnoughItems','mezz/jei/neoforge/JustEnoughItems']]
    save(out/'network-abi.json',{'scope':'STATIC_ABI_ONLY','source':[record(p) for p in originals.values()],'frozenScanRevalidated':record(scan),'classes':counts,'nodes':selected,'completeGraph':record(out/'complete-source-graph.jsonl.gz'),'dispatchNote':'direct method/field/type/Bootstrap edges retained; virtual/interface dynamic reachability is not automatically approved'})
    for side,jar in originals.items():
        for row in selected:
            if row['artifact']!=('forge-jei' if side=='forge' else 'neo-jei'):continue
            name=row['name'].replace('/','.')
            run(out,side+'-'+name,[JDK/'bin/javap','-p','-c','-s','-v','-classpath',jar,name])
    for side in ['forge','neo']:
        deps=sorted((ENV/('runs/convergence-009c-20261017/client-abi/'+side+'/libraries')).rglob('*.jar'));wanted={}
        for jar in deps:
            with zipfile.ZipFile(jar) as archive:
                for name in archive.namelist():
                    if name.endswith('.class') and (name.startswith('net/minecraftforge/network/') or name.startswith('net/neoforged/neoforge/network/') or 'CustomPayloadEvent' in name):
                        assert name not in wanted,(side,name);wanted[name]={'jar':record(jar),'classSHA256':hashlib.sha256(archive.read(name)).hexdigest()}
        save(out/(side+'-network-binaries.json'),wanted);save(out/(side+'-sdk-inputs.json'),[record(p) for p in deps])
        for name in sorted(wanted):
            cls=name[:-6].replace('/','.');run(out,side+'-host-'+cls,[JDK/'bin/javap','-p','-c','-s','-classpath',':'.join(map(str,deps)),cls])
    save(out/'result.json',{'status':'PASS','scope':'STATIC_ABI; not registration or connection','classes':counts,'selectedSourceClasses':len(selected)})
    print('NETWORK_ABI_REVALIDATED originalClasses=1846')
if __name__=='__main__':main()
