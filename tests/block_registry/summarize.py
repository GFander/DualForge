"""Aggregate actual same-artifact Block stages plus complete old and multi-Item regressions."""
import argparse,importlib.util,json
from pathlib import Path
from cohort import consistent
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('multi_summary',REPO/'tests/multi_registry/summarize.py');old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
def summary(historical,multi_stages):
    root=REPO/'out/convergence-006b';index=json.loads((root/'stage-results.json').read_text())
    stages={k:index[k] for k in ('contract','control','treatment')};stages['regression']=str(historical.resolve())
    results={k:json.loads(Path(v).read_text()) for k,v in stages.items()};assert consistent(results),'Block stages/regression mismatch'
    expected=results['treatment']['compat']['SHA256'];old.check_history(results['regression'],expected)
    previous={k:json.loads(Path(v).read_text()) for k,v in json.loads(multi_stages.read_text()).items() if k in ('contract','control','treatment')}
    previous['regression']=results['regression'];assert old.m.consistent(previous),'006A regression cohort mismatch'
    result={'task':'Convergence-006B','006B-S':'PASS','006B-C':'NOT RUN','sameArtifactCohort':True,'compat':results['treatment']['compat'],'fixture':results['treatment']['fixture'],'driver':results['treatment']['driver'],'stages':stages,'multiRegressionStages':str(multi_stages),'historicalStages':results['regression']['results'],'clientExecuted':False}
    (root/'summary.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--historical',type=Path,required=True);p.add_argument('--multi-stages',type=Path,required=True)
    print(json.dumps(summary(**vars(p.parse_args())),indent=2))
