"""Read exact named Minecraft member declarations from each installed client SDK (static only)."""
import argparse
import io
from pathlib import Path
import struct
import sys
import zipfile
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,fresh,record,save

def members(data):
    stream=io.BytesIO(data)
    def u(n):return int.from_bytes(stream.read(n),'big')
    assert u(4)==0xcafebabe
    stream.read(4);count=u(2);pool={};i=1
    while i<count:
        tag=u(1)
        if tag==1:pool[i]=stream.read(u(2)).decode()
        elif tag in [3,4,9,10,11,12,17,18]:stream.read(4)
        elif tag in [5,6]:stream.read(8);i+=1
        elif tag in [7,8,16,19,20]:stream.read(2)
        elif tag==15:stream.read(3)
        else:raise AssertionError(tag)
        i+=1
    stream.read(6);stream.read(2*u(2))
    def attributes():
        for _ in range(u(2)):stream.read(2);stream.read(u(4))
    for _ in range(u(2)):stream.read(6);attributes()
    result=set()
    for _ in range(u(2)):
        access=u(2);name=pool[u(2)];descriptor=pool[u(2)];attributes();result.add((name,descriptor,access))
    return result

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
    expected={
      'net/minecraft/client/Minecraft':[('getInstance','()Lnet/minecraft/client/Minecraft;'),('getLanguageManager','()Lnet/minecraft/client/resources/language/LanguageManager;')],
      'net/minecraft/client/resources/language/LanguageManager':[('getSelected','()Ljava/lang/String;')],
      'net/minecraft/client/resources/language/I18n':[('get','(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;')],
      'net/minecraft/locale/Language':[('getInstance','()Lnet/minecraft/locale/Language;')]}
    results={}
    for side in ['forge','neo']:
        found={name:[] for name in expected}
        for jar in sorted((ENV/('runs/convergence-009c-20261017/client-abi/'+side+'/libraries')).rglob('*.jar')):
            with zipfile.ZipFile(jar) as archive:
                names=set(archive.namelist())
                for name,wanted in expected.items():
                    if name+'.class' not in names:continue
                    data=archive.read(name+'.class');declared=members(data)
                    assert all(any(n==method and d==desc and access&1 for n,d,access in declared) for method,desc in wanted),(side,name,jar)
                    import hashlib
                    found[name].append({'jar':record(jar),'classSHA256':hashlib.sha256(data).hexdigest(),'members':wanted})
        assert all(found.values()),(side,found);results[side]=found
    save(out/'result.json',{'status':'PASS','scope':'STATIC_MEMBER_DECLARATIONS_ONLY; not client execution','results':results})
    print('LOCALE_NATIVE_API_DECLARATIONS_PASS client=NOT_RUN')
if __name__=='__main__':main()
