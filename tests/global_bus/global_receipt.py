"""Exact versioned FnF complete-plan receipt. Never equates planning with a game run."""
import re
VERSION='FNF_CONTRACT_VERSION=global-v1'
ADMISSION='FNF_PLAN_ADMISSION=ACCEPTED targets=3 candidate=COMPOSED_CONTEXT globalListeners=0'
RUNTIME='FNF READ_ONLY COMPLETE PLAN PASS; FNF_RUNTIME=NOT RUN; source classes never defined or initialized'
def validate(text):
 lines=text.splitlines()
 for prefix,expected in [('FNF_CONTRACT_VERSION=',VERSION),('FNF_PLAN_ADMISSION=',ADMISSION)]:
  if [s for s in lines if s.startswith(prefix)]!=[expected]:raise AssertionError('Missing or conflicting '+prefix)
 if [s for s in lines if 'FNF_RUNTIME=' in s]!=[RUNTIME]:raise AssertionError('Invalid runtime status')
 targets=[s for s in lines if s.startswith('FNF_TARGET_VERIFIED=')]
 if any(not re.fullmatch(r'FNF_TARGET_VERIFIED=[A-Za-z0-9_/$]+ originalSHA256=[0-9a-f]{64} outputSHA256=[0-9a-f]{64}',s) for s in targets):raise AssertionError('Invalid target fingerprint proof')
 names=[s.split(' ',1)[0] for s in targets]
 if len(names)!=3 or len(set(names))!=3:raise AssertionError('Incomplete or duplicate target proof')
 return {'version':'global-v1','admission':'ACCEPTED','targets':3,'runtime':'NOT RUN'}
