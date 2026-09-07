"""Rebuild the checked-in small attribute example through independent public tools."""
import hashlib,json,shutil,subprocess,sys
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'scripts'))
import source_yaml
ROOT=Path(__file__).resolve().parents[2];BIN=ROOT/'build/maintained';HERE=Path(__file__).resolve().parent

def write(path,value):path.write_text(source_yaml.dumps(value) if path.suffix=='.yaml' else json.dumps(value,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n')
def command(root,tool,args,status=0):
    p=subprocess.run([str(BIN/tool)]+args,cwd=root,capture_output=True,timeout=30)
    assert p.returncode==status,(tool,p.returncode,p.stderr,p.stdout[-400:]);assert not p.stderr;return p.stdout

def plan(root,ids):
    directory=root/'plan';directory.mkdir(exist_ok=True)
    write(directory/'plan.yaml', {
        'format': 'mundane-plan-yaml-0.1',
        'plans': [{'id':'PLAN-ATTR','context':'logger','baselineScope':'baseline','currentScope':'current'}],
        'activities': [{'id':'ACT-REVIEW','method':'review','objective':'Review logger requirements and their descriptive classification.','expectedEvidence':'Recorded review observations'}],
        'coverage': [{'planId':'PLAN-ATTR','activityId':'ACT-REVIEW','requirementId':id} for id in sorted(ids)]})
    (root/'plan.json').write_bytes(command(root,'mundane-plan',['--root','.','plan']))

def imports(root):
    write(root/'imports.json',{'format':'mundane-imports-0.1','imports':[{'scope':s,'kind':'requirements','path':s+'.json','sha256':hashlib.sha256((root/(s+'.json')).read_bytes()).hexdigest() if s=='baseline' else None,'dependsOn':[]} for s in ['baseline','current']]})

def analyze(root,status=0):
    args=['--root','.','--plan','plan.json','imports.json']
    (root/'linked.json').write_bytes(command(root,'mundane-link',args))
    raw=command(root,'mundane-verify',args,status);(root/'analysis.json').write_bytes(raw);return json.loads(raw)

def render(root):
    # These relative bases resolve within the generated example directory.
    p=subprocess.run([sys.executable,str(ROOT/'experiments/0029-verification-report/render.py'),str(root/'analysis.json'),'--source-base','baseline=baseline-source','--source-base','current=current-source','--source-base','plan=.'],capture_output=True,timeout=30)
    assert p.returncode==0,p.stderr;return p.stdout

def example(root):
    root.mkdir(parents=True,exist_ok=True)
    for scope in ['baseline','current']:
        dest=root/(scope+'-source');dest.mkdir(exist_ok=True)
        shutil.copyfile(ROOT/'examples/attributes/requirement-attributes.yaml',dest/'schema.yaml');shutil.copyfile(ROOT/'examples/attributes/system.mreq.yaml',dest/'source.mreq.yaml')
        if scope=='current':
            p=dest/'source.mreq.yaml';p.write_text(p.read_text().replace('Logger firmware','Controls 😀 <review>'))
            p=dest/'schema.yaml';d=source_yaml.loads(p.read_text());d['attributes']['owner-team']['description']='Revised <team> description';write(p,d)
        command(dest,'mundanereq-validate',['--source=yaml-0.4','--attribute-schema','schema.yaml','source.mreq.yaml'])
        command(dest,'mundanereq-format',['--source=yaml-0.4','--attribute-schema','schema.yaml','--check','source.mreq.yaml'])
        raw=command(dest,'mundanereq-compile',['--source=yaml-0.4','--root','.','--attribute-schema','schema.yaml','source.mreq.yaml']);(root/(scope+'.json')).write_bytes(raw)
    shutil.copytree(ROOT/'examples/attributes/plan',root/'plan',dirs_exist_ok=True)
    (root/'plan.json').write_bytes(command(root,'mundane-plan',['--root','.','plan']))
    imports(root);a=analyze(root,1)
    assert [(r['requirementId'],r['changedAttributes'],r['schemaChanged']) for r in a['coverage']]==[('SYS-001',['owner-team'],True),('SYS-002',[],True)]
    report=render(root);(root/'report.html').write_bytes(report);return a,report

if __name__=='__main__':
    root=ROOT/'build/attribute-example';a,report=example(root)
    for name in ['baseline.json','current.json','plan.json','linked.json','analysis.json','report.html','imports.json']:(root/name).unlink()
    b,rebuilt=example(root);assert b==a and rebuilt==report
    print('PASS checked-in attribute example: validate/format/compile/plan/link/analyze/render, exact baseline pin, source links and identical delete/rebuild')
    print(root/'report.html')
