"""Bind the 006A server conclusion to actual stage results and the complete historical rerun."""
import importlib.util,json
from pathlib import Path
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('multi',Path(__file__).with_name('run_multi_registry.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def check_history(result,expected):
    assert result['status']=='PASS'
    if 'compat' in result:assert result['compat']['SHA256']==expected,'Historical result compat mismatch'
    for path in result.get('results',{}).values():check_history(json.loads(Path(path).read_text()),expected)
    for child in result.get('regressions',{}).values():
        assert child['status']=='PASS' and child['compatSha256']==expected,'Historical child compat mismatch'
        check_history(json.loads(Path(child['result']).read_text()),expected)

def summarize(history):
    root=REPO/'out/convergence-006a';index=json.loads((root/'stage-results.json').read_text())
    stages={s:index[s] for s in ('contract','control','treatment')};stages['regression']=str(history.resolve())
    results={s:json.loads(Path(p).read_text()) for s,p in stages.items()}
    same=m.consistent(results)
    historical=results['regression']
    check_history(historical,results['treatment']['compat']['SHA256'])
    fixture=json.loads((root/'fixture.json').read_text())
    assert results['treatment']['fixture']['SHA256']==fixture['jars']['multi-registry-probe']['SHA256']
    assert results['treatment']['driver']['SHA256']==fixture['jars']['multi-registry-driver']['SHA256']
    result={'task':'Convergence-006A','006A-S':'PASS' if same else 'FAIL','006A-C':'NOT RUN','sameArtifactCohort':same,
            'compat':results['treatment']['compat'],'fixture':results['treatment']['fixture'],'driver':results['treatment']['driver'],
            'stages':stages,'historicalStages':historical['results'],'clientExecuted':False,'baseline':str(root/'baseline.json')}
    m.w.save(root/'summary.json',result);assert same,'No mixed stages may become overall PASS';return result
if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--history-result',type=Path,required=True)
    print(json.dumps(summarize(p.parse_args().history_result),indent=2))
