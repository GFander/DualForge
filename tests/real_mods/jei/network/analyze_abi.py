"""Derive method/Bootstrap graph from revalidated original ClassFiles; never approve dynamic reachability."""
import argparse
import gzip
import json
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import fresh,record,save

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--abi',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
    with gzip.open(a.abi/'complete-source-graph.jsonl.gz','rt') as f:rows=[json.loads(line) for line in f]
    graph={};definitions={}
    for r in rows:
        for m in r['code']:
            key=r['artifact']+':'+r['name']+'.'+m['name']+m['descriptor'];edges=[];definitions[key]=m
            for op in m['instructions']:
                if op.get('owner') and op['opcode'] in [182,183,184,185]:edges.append({'kind':'DIRECT_CALL','index':op['index'],'opcode':op['opcode'],'owner':op['owner'],'name':op['name'],'descriptor':op['descriptor']})
                for arg in op.get('arguments',[]):
                    if isinstance(arg,dict) and 'handle' in arg:edges.append({'kind':'BOOTSTRAP_HANDLE','index':op['index'],**arg['handle']})
            graph[key]={'classSHA256':r['classSHA256'],'edges':edges}
    selected={};queue=[key for key in graph if (':mezz/jei/forge/network/NetworkHandler.' in key or ':mezz/jei/neoforge/network/NetworkHandler.' in key)]
    while queue:
        key=queue.pop()
        if key in selected:continue
        selected[key]=graph[key]
        prefix=key.split(':')[0]+':'
        queue.extend(target for edge in graph[key]['edges'] if (target:=prefix+edge['owner']+'.'+edge['name']+edge['descriptor']) in graph and target not in selected)
    packets=[]
    for r in rows:
        if '/network/packets/Packet' in r['name']:
            clinit=next((m['instructions'] for m in r['code'] if m['name']=='<clinit>'),[])
            packets.append({'artifact':r['artifact'],'class':r['name'],'classSHA256':r['classSHA256'],'idConstants':[op['constant'] for op in clinit if isinstance(op.get('constant'),str)],'codecInputs':[op for op in clinit if op.get('owner') and (op.get('name')=='STREAM_CODEC' or '/codec/' in op['owner'] or 'EnumStreamCodec' in op['owner'])]})
    save(out/'network-call-graph.json',{'status':'STATIC_ABI','inputs':[record(a.abi/'complete-source-graph.jsonl.gz')],'methodGraph':selected,'allMethodGraph':graph,'packets':packets,'limits':'Direct and Bootstrap edges are complete for revalidated inputs. Interface dispatch, inherited methods and Dist reachability require further proof; not a production closure approval.'})
    save(out/'result.json',{'status':'PASS','scope':'STATIC_ABI','allMethods':len(graph),'networkReachableDeclaredMethods':len(selected),'packets':len(packets)})
    print(json.dumps({'allMethods':len(graph),'networkReachableDeclaredMethods':len(selected),'packets':len(packets)}))
if __name__=='__main__':main()
