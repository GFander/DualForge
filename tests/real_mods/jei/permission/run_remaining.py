"""Run the final-artifact 010G historical regression cohort serially. Frozen inputs are reused; results are new."""
import argparse
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,ROOT,fresh,record,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for n in ['jar','output']:p.add_argument('--'+n,type=Path,required=True)
    a=p.parse_args();out=fresh(a.output);tools=ROOT/'tests/real_mods/jei';b=ENV/'runs/convergence-010b';c=ENV/'runs/convergence-010c';d=ENV/'runs/convergence-010d';e=ENV/'runs/convergence-010e';f=ENV/'runs/convergence-010f'
    groups=[
        ('network-observer-build',tools/'network/build_observer.py',['--output',out/'network-observers']),
        ('network-contract',tools/'network/run_contract.py',['--jar',a.jar,'--fixtures',d/'fixture-build-2','--output',out/'network-contract']),
        ('network-boundary',tools/'network/run_boundary.py',['--jar',a.jar,'--fixture',d/'fixture-build-2/netalpha/netalpha.jar','--output',out/'network-boundary']),
        ('network-live',tools/'network/run_live.py',['--jar',a.jar,'--fixtures',d/'fixture-build-2','--observers',out/'network-observers','--output',out/'network-live']),
        ('constructor-contract',tools/'constructor/run_contract.py',['--jar',a.jar,'--fixtures',c/'fixture-build-2','--output',out/'constructor-contract']),
        ('constructor-boundary',tools/'constructor/run_boundary.py',['--jar',a.jar,'--fixture',c/'fixture-build-2/localpha/localpha.jar','--output',out/'constructor-boundary']),
        ('dual-contract',tools/'dual_bus/run_contract.py',['--jar',a.jar,'--fixtures',b/'fixture-build-2','--output',out/'dual-contract']),
        ('constructor-live',tools/'constructor/run_live.py',['--jar',a.jar,'--fixtures',c/'fixture-build-2','--observers',c/'observers-final','--output',out/'constructor-live']),
        ('original-config-live',tools/'constructor/run_config_live.py',['--jar',a.jar,'--fixtures',c/'original-config-fixture','--observers',c/'config-observers','--output',out/'original-config-live']),
        ('dual-live',tools/'dual_bus/run_live.py',['--jar',a.jar,'--fixtures',b/'fixture-build-2','--observers',b/'observer-build-5','--output',out/'dual-live']),
        ('dual-owner',ROOT/'tests/real_mods/clumps/run_server.py',['--host','neoforge','--install',ENV/'neoforge-21.1.256','--jdk-home',JDK,'--output',out/'dual-owner','--mod',a.jar,'--mod',b/'fixture-build-2/busalpha/busalpha.jar','--mod',b/'fixture-build-2/busbeta/busbeta.jar','--mod',b/'observer-build-final/neo/dualobserver.jar']),
        ('closure-contract',tools/'network_closure/run_contract.py',['--jar',a.jar,'--fixtures',e/'fixture-build-3','--output',out/'closure-contract']),
        ('closure-boundary',tools/'permission/run_e_boundary.py',['--jar',a.jar,'--fixture',e/'fixture-build-3/flowalpha/flowalpha.jar','--output',out/'closure-boundary']),
        ('closure-live',tools/'network_closure/run_live.py',['--jar',a.jar,'--fixtures',e/'fixture-build-3','--observers',e/'observers-final','--output',out/'closure-live']),
        ('derived-contract',tools/'permission/run_f_contract.py',['--jar',a.jar,'--fixtures',f/'fixture-build-1','--output',out/'derived-contract']),
        ('derived-boundary',tools/'permission/run_boundary.py',['--jar',a.jar,'--fixture',f/'fixture-build-1/treealpha/treealpha.jar','--output',out/'derived-boundary']),
        ('derived-live',tools/'derived_payload/run_live.py',['--jar',a.jar,'--fixtures',f/'fixture-build-1','--observers',f/'observers-final','--output',out/'derived-live']),
        ('mouse',ROOT/'tests/real_mods/mouse_tweaks/run_final_checks.py',['--jar',a.jar,'--run-root',ENV/'runs/convergence-009c-20261017','--fixture',ENV/'runs/convergence-009c-20261017/fixture-build-2/gesture-input.jar','--output',out/'mouse']),
        ('helper-ci',ROOT/'tests/real_mods/mouse_tweaks/run_checked_tool.py',['--script',ROOT/'tests/helper_registry/run_ci_contract.py','--','--java-home',JDK,'--libraries',ENV/'neoforge-21.1.256/libraries','--output',out/'helper-ci']),
        ('local',tools/'constructor/run_local.py',['--output',out/'local']),
        ('jei',tools/'permission/run_jei.py',['--jar',a.jar,'--output',out/'jei']),
    ]
    completed=[];save(out/'inputs.json',[record(a.jar),record(Path(__file__))])
    for label,script,argv in groups:
        print('START '+label,flush=True);run(out,label+'-driver',[sys.executable,'-B',script,*argv]);completed.append(label)
        run(out,label+'-retention',[sys.executable,'-B',tools/'permission/retain_evidence.py','--run',ENV/'runs/convergence-010g','--receipt',out/(label+'-retention.json')])
        save(out/'result.json',{'status':'IN_PROGRESS','jar':record(a.jar),'completed':completed});print('PASS '+label,flush=True)
    save(out/'result.json',{'status':'PASS','jar':record(a.jar),'completed':completed,'realConnection':'NOT_RUN','realPacketDelivery':'NOT_RUN'})
if __name__=='__main__':main()
