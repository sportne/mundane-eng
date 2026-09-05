"""Checked, one-time repository conversion and immutable replay; no YAML reformatter."""
import copy
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import urllib.parse

ROOT=Path(__file__).resolve().parents[2]
BINARY=ROOT/'build/maintained/mundane-work'
INVENTORY=ROOT/'experiments/0035-work-yaml/migration.json'

def sha(data):return hashlib.sha256(data).hexdigest()
def encoded(value):return (json.dumps(value,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n').encode()
def git_bytes(commit,path):return subprocess.check_output(['git','show',commit+':'+path],cwd=ROOT)

def retarget(text,path,mapping):
    edits=[]
    def replace(match):
        dest=match.group(1);base,sep,fragment=dest.partition('#')
        if not base or '://' in base or base.startswith(('/', 'mailto:')):return match.group(0)
        target=os.path.normpath(str(Path(path).parent/urllib.parse.unquote(base)))
        if target not in mapping:return match.group(0)
        updated=urllib.parse.quote(os.path.relpath(mapping[target],Path(path).parent),safe='/._-')+(sep+fragment if sep else '')
        edits.append({'from':dest,'to':updated})
        return ']('+updated+')'
    return re.sub(r'\]\(([^)\n]+)\)',replace,text),edits

def emit(values):
    """A narrow emitter: quoted scalars, block collections, exact literal body strings."""
    v={'format':'mundane-work-yaml-0.2',**values}
    def scalar(value):return json.dumps(value,ensure_ascii=False)
    def lines(value,indent=0):
        pad=' '*indent;out=[]
        for key,value in value.items():
            if key=='body' and isinstance(value,str) and not any(c in value for c in ['\r','\x85','\u2028','\u2029']) and not any(ord(c)<32 and c not in '\n\t' for c in value):
                out.append(pad+key+(': |2+\n' if value.endswith('\n') else ': |2-\n'))
                for line in value.splitlines(keepends=True):out.append('\n' if line=='\n' else pad+'  '+line)
                if not value.endswith('\n'):out.append('\n')
            elif isinstance(value,dict) and value:
                out.append(pad+key+':\n');out.extend(lines(value,indent+2))
            elif isinstance(value,list) and value:
                out.append(pad+key+':\n')
                for item in value:
                    if isinstance(item,dict):
                        nested=lines(item,indent+4);nested[0]=pad+'  - '+nested[0][indent+4:];out.extend(nested)
                    else:out.append(pad+'  - '+scalar(item)+'\n')
            else:
                # JSON double-quoted escapes preserve control/line separator values exactly.
                out.append(pad+key+': '+json.dumps(value,ensure_ascii=True)+'\n')
        return out
    # Stable authoring order, independent of the sorted compiled object representation.
    ordered={key:v[key] for key in ['format','id','kind','title','status','dependencies','relations','planning','body']}
    return ''.join(lines(ordered)).encode()

def compile_at(root,manifest):
    (root/'selection.json').write_bytes(encoded(manifest))
    r=subprocess.run([str(BINARY),'compile','--root',str(root),str(root/'selection.json')],cwd=ROOT,capture_output=True,timeout=60)
    assert r.returncode==0,(r.stderr,r.stdout[:2000])
    return {item['location']['path']:item['values'] for item in json.loads(r.stdout)['items']}

def conversion(commit):
    selection=json.loads(git_bytes(commit,'roadmap/work-items.json'))
    assert selection['format']=='mundane-work-set-0.1'
    files=selection['files']+['roadmap/task-card-template.md']
    mapping={p:str(Path(p).with_suffix('.yaml')) for p in files}
    originals={p:git_bytes(commit,p) for p in files}
    with tempfile.TemporaryDirectory(prefix='work-yaml-conversion-') as folder:
        root=Path(folder)
        for p,data in originals.items():(root/p).parent.mkdir(parents=True,exist_ok=True);(root/p).write_bytes(data)
        before=compile_at(root,{'format':'mundane-work-set-0.1','files':files})
        converted={};records=[];expected={}
        for p in files:
            v=copy.deepcopy(before[p]);edits=[]
            for key in ['body','title']:
                v[key],changes=retarget(v[key],p,mapping);edits.extend({'field':key,**c} for c in changes)
            for key,text in v['planning'].items():
                v['planning'][key],changes=retarget(text,p,mapping);edits.extend({'field':'planning.'+key,**c} for c in changes)
            for i,relation in enumerate(v['relations']):
                if relation['relation']=='evidence' and relation['target'] in mapping:
                    old=relation['target'];relation['target']=mapping[old];edits.append({'field':f'relations.{i}.target','from':old,'to':relation['target']})
            v['relations'].sort(key=lambda x:json.dumps(x,ensure_ascii=False,sort_keys=True,separators=(',',':')))
            data=emit(v);new=mapping[p];converted[new]=data;expected[new]=v
            (root/new).write_bytes(data)
            records.append({'id':v['id'],'before':p,'after':new,'sourceSha256':sha(originals[p]),'yamlSha256':sha(data),'originalValuesSha256':sha(encoded(before[p])),'valuesSha256':sha(encoded(v)),'bodySha256':sha(v['body'].encode()),'linkEdits':edits})
        after=compile_at(root,{'format':'mundane-work-set-0.2','source':'mundane-work-yaml-0.2','files':list(converted)})
        assert after==expected,'migration changed decoded values'
    return originals,converted,{'baseline':commit,'cards':records},mapping

def main():
    assert sys.argv[1:] in ([],['--write']),'usage: migrate.py [--write]'
    if not sys.argv[1:]:
        inventory=json.loads(INVENTORY.read_bytes());_,_,actual,_=conversion(inventory['baseline']);assert actual==inventory
        print('PASS immutable YAML conversion replay:',len(inventory['cards'])-1,'cards plus template; exact decoded values and recorded link edits')
        return
    assert not INVENTORY.exists(),'conversion already recorded; edit YAML sources directly'
    commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    originals,converted,inventory,mapping=conversion(commit)
    # Preflight every input/output before publishing any path changes.
    for p,data in originals.items():assert (ROOT/p).read_bytes()==data,('uncommitted source edit',p)
    for p in converted:assert not (ROOT/p).exists(),('output exists',p)
    changes={p:data for p,data in converted.items()}
    files=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard'],cwd=ROOT,text=True).splitlines()
    incoming=[]
    for p in sorted(set(files)):
        file=ROOT/p
        if file.suffix!='.md' or p in originals or p=='WORK-ITEMS.md' or not file.is_file():continue
        old=file.read_text();updated,edits=retarget(old,p,mapping)
        if edits:changes[p]=updated.encode();incoming.append({'path':p,'linkEdits':edits})
    changes['roadmap/work-items.json']=encoded({'format':'mundane-work-set-0.2','source':'mundane-work-yaml-0.2','files':[mapping[p] for p in json.loads((ROOT/'roadmap/work-items.json').read_bytes())['files']]})
    changes[str(INVENTORY.relative_to(ROOT))]=json.dumps(inventory,indent=2,ensure_ascii=False).encode()+b'\n'
    changes['experiments/0035-work-yaml/incoming-links.json']=json.dumps(incoming,indent=2,ensure_ascii=False).encode()+b'\n'
    # Recheck originals after candidate validation. Restore exact bytes on a local write failure.
    for p,data in originals.items():assert (ROOT/p).read_bytes()==data
    backups={p:(ROOT/p).read_bytes() if (ROOT/p).exists() else None for p in changes.keys()|originals.keys()}
    try:
        for p,data in changes.items():(ROOT/p).write_bytes(data)
        for p in originals:(ROOT/p).unlink()
    except BaseException:
        for p,data in backups.items():
            if data is None:(ROOT/p).unlink(missing_ok=True)
            else:(ROOT/p).write_bytes(data)
        raise
    print('PASS converted',len(originals)-1,'cards plus template; all candidate values verified before write;',len(incoming),'documents retargeted')

if __name__=='__main__':main()
