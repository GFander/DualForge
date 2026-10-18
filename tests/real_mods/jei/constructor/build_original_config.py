"""Forge-only integration fixture: unchanged actual JEI config classes inside a new controlled owner."""
import argparse
import hashlib
from pathlib import Path
import sys
import zipfile
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
    original=ENV/'runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar';assert hashlib.sha256(original.read_bytes()).hexdigest()=='f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5'
    deps=sorted((ENV/'runs/convergence-009c-20261017/client-abi/forge/libraries').rglob('*.jar'));classes=out/'classes';classes.mkdir();src=out/'src';src.mkdir();files=[]
    templates=[Path(__file__).parent/'fixture'/name for name in ['Entry.java.in','Subscriptions.java.in','LocaleSource.java.in','Translator.java.in']]
    for template in templates:
        text=template.read_text().replace('@PACKAGE@','org.example.originalcfg').replace('@ID@','originalcfg').replace('@DEATH_REGISTER@','register')
        if template.name=='Entry.java.in':
            text=text.replace('import java.util.function.Consumer;','import java.util.function.Consumer;\nimport mezz.jei.common.config.IServerConfig;\nimport mezz.jei.forge.config.ServerConfig;')
            text=text.replace('ISettings','IServerConfig').replace('Settings.register','ServerConfig.register')
            text=text.replace('settings.enabled()','settings.isCheatModeEnabledForOp()').replace('settings.secondary()','settings.isCheatModeEnabledForCreative()').replace('settings.cancelDeath()','settings.isCheatModeEnabledForGive()')
        target=src/template.name.removesuffix('.in');target.write_text(text);files.append(target)
    save(out/'inputs.json',[record(p) for p in [original,*templates,*deps,Path(__file__)]])
    run(out,'javac',[JDK/'bin/javac','--release','21','-proc:none','-cp',':'.join(map(str,[original,*deps])),'-d',classes,*files])
    copied=[]
    with zipfile.ZipFile(original) as archive:
        for name in ['mezz/jei/common/config/IServerConfig.class','mezz/jei/forge/config/ServerConfig.class']:
            data=archive.read(name);target=classes/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data);copied.append({'member':name,**record(target)})
    meta=classes/'META-INF';meta.mkdir();(meta/'mods.toml').write_text('modLoader="javafml"\nloaderVersion="[52,)"\nlicense="test-only-component-integration"\n[[mods]]\nmodId="originalcfg"\nversion="1"\n')
    jar=out/'originalcfg.jar';run(out,'jar',[JDK/'bin/jar','--create','--file',jar,'-C',classes,'.'])
    save(out/'result.json',{'status':'PASS','scope':'ORIGINAL_JEI_CONFIG_COMPONENT_IN_NEW_FORGE_OWNER; not full JEI','jar':record(jar),'unchangedOriginalClasses':copied})
if __name__=='__main__':main()
