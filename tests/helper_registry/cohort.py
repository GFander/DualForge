"""Reject mixing Contract/Control/Treatment compat and fixture identities."""
def validate(contract,control,treatment):
    stages=[contract,control,treatment]
    if any(v.get('status')!='PASS' for v in stages):raise ValueError('Required stage not PASS')
    for key in ('compatSHA256','fixtureSHA256'):
        values=[v.get(key) for v in stages]
        if not all(isinstance(v,str) and len(v)==64 for v in values) or len(set(values))!=1:raise ValueError('Cohort mismatch: '+key)
    if control.get('installedJARs')!=treatment.get('installedJARs'):raise ValueError('Installed JAR cohort mismatch')
    if control.get('observerSHA256')!=treatment.get('observerSHA256'):raise ValueError('Observer cohort mismatch')
    if control.get('fixtureEnvironment')!={'DF4B_REGISTER':'false'} or treatment.get('fixtureEnvironment')!={'DF4B_REGISTER':'true'}:raise ValueError('Unexpected C/T condition')
    return {'status':'PASS','compatSHA256':stages[0]['compatSHA256'],'fixtureSHA256':stages[0]['fixtureSHA256']}
