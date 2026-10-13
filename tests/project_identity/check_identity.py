"""Check active naming and per-file historical protection, with explicit migration exceptions."""
import hashlib
import json
from pathlib import Path
import re
import subprocess

REPO = Path(__file__).resolve().parents[2]
INDEX = REPO / 'Dev Manuals/DualForge 命名统一迁移索引.json'


def normalize(data, mapping):
    for old, new in mapping:
        data = data.replace(old.encode(), new.encode())
    return data


def check():
    index = json.loads(INDEX.read_text())
    paths = set(subprocess.check_output(['git','ls-files','-z','--cached','--others','--exclude-standard'], cwd=REPO).decode().strip('\0').split('\0'))
    protected = {item['path']: item for item in index['protected']}
    documents = {item['path']: item for item in index['migrationDocumentExemptions']}
    lines = {(item['path'], item['text']): item['reason'] for item in index['lineExemptions']}
    errors = []
    for name in sorted(paths):
        path = REPO / name
        if not path.is_file():
            errors.append('Missing tracked file: ' + name); continue
        data = path.read_bytes()
        if name in protected:
            if hashlib.sha256(data).hexdigest() != protected[name]['sha256']:
                errors.append('Protected bytes changed: ' + name)
            continue
        if name in documents: continue
        try: text = data.decode()
        except UnicodeError: continue
        for line in text.splitlines():
            if any(term.lower() in line.lower() for term in index['forbiddenNames']) and (name, line) not in lines:
                errors.append('Unapproved old identity: ' + name + ': ' + line)
        if any(term.lower() in name.lower() for term in index['forbiddenNames']):
            errors.append('Unapproved old path: ' + name)
    old_files = subprocess.check_output(['git','ls-tree','-r','--name-only',index['baseCommit'],index['oldProductionRoot']],cwd=REPO,text=True).splitlines()
    for old in old_files:
        new = old
        for a,b in index['pathMapping']: new = new.replace(a,b)
        original = subprocess.check_output(['git','show', index['baseCommit'] + ':' + old],cwd=REPO)
        # Verify the frozen identity migration contract, independently of later authorized features.
        migrated = subprocess.check_output(['git','show',index['productionCommit'] + ':' + new],cwd=REPO)
        if migrated != normalize(original,index['symbolMapping']):
            errors.append('Production change beyond identity mapping: ' + new)
    if errors: raise AssertionError('\n'.join(errors))
    return {'status':'PASS','scannedFiles':len(paths),'protectedFiles':len(protected),'frozenMigrationProductionFiles':len(old_files),'lineExemptions':len(lines)}


if __name__ == '__main__':
    print(json.dumps(check(),ensure_ascii=False,indent=2))
