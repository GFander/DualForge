"""Reject mixing Contract/Control/Treatment compat and fixture identities."""
import re

def digest(value):
    return isinstance(value,str) and re.fullmatch('[a-f0-9]{64}',value) is not None

def validate(contract,control,treatment,register_environment="DF4B_REGISTER"):
    stages=[contract,control,treatment]
    if any(v.get('status')!='PASS' for v in stages):raise ValueError('Required stage not PASS')
    for key in ('compatSHA256','fixtureSHA256'):
        values=[v.get(key) for v in stages]
        if not all(digest(v) for v in values) or len(set(values))!=1:raise ValueError('Cohort mismatch: '+key)
    installed=control.get('installedJARs')
    if not isinstance(installed,dict) or not installed or installed!=treatment.get('installedJARs') or not all(digest(v) for v in installed.values()):raise ValueError('Installed JAR cohort mismatch')
    if not digest(control.get('observerSHA256')) or control.get('observerSHA256')!=treatment.get('observerSHA256'):raise ValueError('Observer cohort mismatch')
    if not {contract['compatSHA256'],contract['fixtureSHA256'],control['observerSHA256']}.issubset(installed.values()):raise ValueError('Installed JAR receipts do not contain the asserted cohort')
    if control.get('fixtureEnvironment')!={register_environment:'false'} or treatment.get('fixtureEnvironment')!={register_environment:'true'}:raise ValueError('Unexpected C/T condition')
    return {'status':'PASS','compatSHA256':stages[0]['compatSHA256'],'fixtureSHA256':stages[0]['fixtureSHA256']}
