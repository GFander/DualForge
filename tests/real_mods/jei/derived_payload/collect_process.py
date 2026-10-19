"""Complete source method edges reachable from each real process; unresolved host/dynamic calls stay explicit."""
import argparse,gzip,json,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import fresh,record,save

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--graph',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
 source={r['name']:r for r in map(json.loads,gzip.open(a.graph,'rt')) if r['artifact']=='forge-jei'}
 def resolve(owner,name,desc):
  seen=set()
  while owner in source:
   assert owner not in seen;seen.add(owner);row=source[owner]
   if any(m['name']==name and m['descriptor']==desc for m in row['methods']):return owner
   if name=='<init>':return None
   owner=row['superclass']
  return None
 network=source['mezz/jei/forge/network/NetworkHandler'];directions={};current=None
 for method in network['code']:
  if method['name']!='<init>':continue
  for op in method['instructions']:
   if op.get('name') in ['serverbound','clientbound'] and op.get('opcode')==185:current=op['name'].upper()
   if current and op.get('opcode')==178 and op.get('descriptor')=='Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$Type;':directions[op['owner']]=current
 packets=[]
 for owner,direction in directions.items():
  row=source[owner];root=next(m for m in row['methods'] if m['name']=='process');pending=[(owner,'process',root['descriptor'])];seen=set();methods=[];edges=[]
  while pending:
   cls,name,desc=pending.pop()
   if (cls,name,desc) in seen:continue
   seen.add((cls,name,desc));body=next(m for m in source[cls]['code'] if m['name']==name and m['descriptor']==desc);decl=next(m for m in source[cls]['methods'] if m['name']==name and m['descriptor']==desc);methods.append({'owner':cls,'name':name,'descriptor':desc,'access':decl['access'],'sourceClassSHA256':source[cls]['classSHA256'],'code':body})
   calls=[]
   for op in body['instructions']:
    if op.get('nodeType')==5:
     calls.append((op['owner'],op['name'],op['descriptor'],'CALL',op['opcode']))
     if op['opcode']==184 and op['owner'] in source and any(m['name']=='<clinit>' for m in source[op['owner']]['methods']):calls.append((op['owner'],'<clinit>','()V','ACTIVE_CLASS_INITIALIZATION',184))
    active=op.get('owner') if op.get('opcode') in [178,179] else op.get('type') if op.get('opcode')==187 else None
    if active in source and any(m['name']=='<clinit>' for m in source[active]['methods']):calls.append((active,'<clinit>','()V','ACTIVE_CLASS_INITIALIZATION',op['opcode']))
    if op.get('nodeType')==6:
     for item in [op.get('bootstrap',{}),*op.get('arguments',[])]:
      h=item.get('handle')
      if h and h['tag']>=5:calls.append((h['owner'],h['name'],h['descriptor'],'BOOTSTRAP_HANDLE',h['tag']))
   for target,name2,desc2,kind,opcode in calls:
    resolved=resolve(target,name2,desc2);edge={'from':[cls,name,desc],'to':[target,name2,desc2],'kind':kind,'opcodeOrTag':opcode,'resolvedSourceOwner':resolved,'hostABIProved':False,'dynamicDispatchProved':False};edges.append(edge)
    if resolved:pending.append((resolved,name2,desc2))
  overrides=[]
  for method in row['methods']:
   if method['name'].startswith('<'):continue
   parent=resolve(row['superclass'],method['name'],method['descriptor']);overrides.append({'name':method['name'],'descriptor':method['descriptor'],'declaredOwner':owner,'parentDeclaration':parent,'category':'OVERRIDDEN_SOURCE_METHOD' if parent else 'DECLARED_METHOD','bridge':method['bridge'],'synthetic':method['synthetic']})
  packets.append({'packet':owner,'direction':direction,'process':root,'superclass':row['superclass'],'interfaces':row['interfaces'],'methodResolution':overrides,'reachableSourceMethods':methods,'edges':edges,'limit':'static declared targets only; interface/default/virtual runtime implementations, host business semantics, permissions and player identity NOT_PROVED'})
 assert len(packets)==6
 save(out/'packet-process-closure.json',{'scope':'STATIC_SOURCE_GRAPH_NOT_BUSINESS_APPROVAL','input':record(a.graph),'packets':packets});save(out/'result.json',{'status':'PASS_STATIC_COLLECTION','packets':len(packets),'sourceMethodVisits':sum(len(p['reachableSourceMethods']) for p in packets),'edges':sum(len(p['edges']) for p in packets),'dynamicBusiness':'BLOCKED'});print('PROCESS_GRAPH_COLLECTED',len(packets))
if __name__=='__main__':main()
