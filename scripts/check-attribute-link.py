"""Serialized attribute boundary: actual scoped/pinned JVM/native and parser-free consumers."""
import copy,hashlib,json,shutil,subprocess,tempfile
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import source_yaml
ROOT=Path(__file__).resolve().parents[1];BIN=ROOT/'build/maintained';CP=BIN/'classes';GOLD=ROOT/'experiments/0036-project-attributes/golden'
BASE=source_yaml.loads((ROOT/'examples/attributes/requirement-attributes.yaml').read_text());SOURCE=(ROOT/'examples/attributes/system.mreq.yaml').read_text()
def write(p,a):p.write_text(source_yaml.dumps(a) if p.suffix=='.yaml' else json.dumps(a,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n')
def run(command,args,root,status=0):
 p=subprocess.run(command+args,cwd=root,capture_output=True,timeout=30);assert p.returncode==status,(command,p.returncode,p.stderr,p.stdout[-600:]);assert not p.stderr;return p.stdout
with tempfile.TemporaryDirectory(prefix='attribute-link-') as temp:
 root=Path(temp);schema=root/'schema.yaml';source=root/'source.mreq.yaml';write(schema,BASE);source.write_text(SOURCE)
 def compile():return json.loads(run([str(BIN/'mundanereq-compile')],['--source=yaml-0.4','--root','.','--attribute-schema','schema.yaml','source.mreq.yaml'],root))
 original=compile();write(root/'baseline.json',original);write(root/'current.json',original)
 (root/'plan').mkdir();
 (root/'plan/plan.tsv').write_text('format\tplan_id\tcontext\tbaseline_scope\tcurrent_scope\nmundane-plan-source-0.1\tPLAN-ATTR\tlogger\tbaseline\tcurrent\n')
 (root/'plan/activities.tsv').write_text('activity_id\tmethod\tobjective\texpected_evidence\nACT-REVIEW\treview\tReview logger requirements and their descriptive classification.\tRecorded review observations\n')
 (root/'plan/coverage.tsv').write_text('plan_id\tactivity_id\trequirement_id\nPLAN-ATTR\tACT-REVIEW\tSYS-001\nPLAN-ATTR\tACT-REVIEW\tSYS-002\n')
 (root/'plan.json').write_bytes(run([str(BIN/'mundane-plan')],['--root','.','plan'],root))
 manifest={'format':'mundane-imports-0.1','imports':[{'scope':scope,'path':scope+'.json','kind':'requirements','sha256':None,'dependsOn':[]} for scope in ['baseline','current']]};write(root/'imports.json',manifest)
 # Remove every requirement/source adapter and YAML library from the consumer classpath.
 isolated=root/'classes';shutil.copytree(CP,isolated)
 for p in (isolated/'mundanereq').rglob('*.class'):
  if p.name!='Versions.class':p.unlink()
 for glob in ['WorkCompiler*.class','WorkYaml*.class']:
  for p in (isolated/'engineering/work').glob(glob):p.unlink()
 for p in (isolated/'engineering/verification').glob('PlanCompiler*.class'):p.unlink()
 def commands(tool,main):return [[str(BIN/tool)],['java','-cp',str(isolated),main]]
 args=['--root','.','--plan','plan.json','imports.json']
 def invoke(tool,main,status=0):
  results=[run(c,args,root,status) for c in commands(tool,main)];assert results[0]==results[1];return json.loads(results[0])
 def analyze(status=0):return invoke('mundane-verify','engineering.verification.VerifyMain',status)
 linked=invoke('mundane-link','engineering.artifacts.LinkMain');a=analyze();assert linked['format']=='mundane-linked-0.2' and a['format']=='mundane-verification-0.2'
 assert all(r['changedFields']==[] and r['changedAttributes']==[] and r['schemaChanged'] is False for r in a['coverage'])
 # Scope independence, exact source recompilation, value changes, and conservative whole-schema changes.
 source.write_text(SOURCE.replace('Logger firmware','Controls 😀 <review>'));current=compile();write(root/'current.json',current);a=analyze(1)
 assert [(r['requirementId'],r['changedFields'],r['changedAttributes']) for r in a['coverage'] if r['state']=='review-stale']==[('SYS-001',['attributes'],['owner-team'])]
 value_result=copy.deepcopy(a)
 for change in [lambda d:d['attributes']['owner-team'].update(description='Revised <team> description'),lambda d:d['attributes']['discipline']['values'].append('unassigned'),lambda d:d['attributes']['discipline'].update(required=False),lambda d:d['attributes'].update(note={'type':'text','required':False,'description':'Optional note'})]:
  source.write_text(SOURCE);d=copy.deepcopy(BASE);change(d);write(schema,d);write(root/'current.json',compile());a=analyze(1)
  assert all(r['changedFields']==['attributeSchema'] and r['schemaChanged'] and r['changedAttributes']==[] for r in a['coverage'])
 # Golden combines actual value + description changes; retained schemas have the same name and different definitions.
 d=copy.deepcopy(BASE);d['attributes']['owner-team']['description']='Revised <team> description';write(schema,d);source.write_text(SOURCE.replace('Logger firmware','Controls 😀 <review>'));write(root/'current.json',compile());a=analyze(1)
 assert a==json.loads((GOLD/'verification.json').read_text())
 # Comment/order-only snapshots and changed paths are provenance, not meaning.
 d=copy.deepcopy(BASE);d['attributes']['discipline']['values'].reverse();write(schema,d);source.write_text('# comment\n'+SOURCE);a=compile();a['attributeSchema']['source']['path']='moved-schema.yaml'
 for span in a['attributeSchema']['locations'].values():span['path']='moved-schema.yaml'
 write(root/'current.json',a);assert all(r['state']=='current' for r in analyze()['coverage'])
 # Required optional field in a new declaration is invalid for SYS-002 and publishes no records.
 d=copy.deepcopy(BASE);d['attributes']['owner-team']['required']=True;write(schema,d)
 invalid=json.loads(run([str(BIN/'mundanereq-compile')],['--source=yaml-0.4','--root','.','--attribute-schema','schema.yaml','source.mreq.yaml'],root,1));assert not invalid['complete'] and invalid['requirements']==[]
 # Strict serialized rejection is independent of source validation or completeness claims.
 mutations=[lambda a:a['requirements'][0]['values']['attributes'].update(unknown='x'),lambda a:a['requirements'][0]['values']['attributes'].update(discipline='bad'),lambda a:a['requirements'][0]['values']['attributes'].pop('discipline'),lambda a:a['requirements'][0]['values']['attributes'].update(discipline=3),lambda a:a['requirements'][0]['locations']['attributes'].pop('discipline'),lambda a:a['requirements'][0]['locations']['attributes']['discipline']['value'].update(path='absent'),lambda a:a['requirements'][0]['locations']['attributes']['discipline']['name']['start'].update(line=0),lambda a:a['attributeSchema']['definition'].update(format='future'),lambda a:a['attributeSchema']['definition']['attributes']['discipline'].update(required='true'),lambda a:a['attributeSchema']['locations'].pop('discipline'),lambda a:a['attributeSchema']['source'].update(sha256='bad'),lambda a:a.update(complete=False),lambda a:a.update(sourceContract='mundanereq-yaml-0.3'),lambda a:a.pop('attributeSchema'),lambda a:a.update(attributeSchema=None)]
 for change in mutations:
  a=copy.deepcopy(original);change(a);write(root/'current.json',a);bad=analyze(2);assert not bad['complete'] and not bad['coverage'] and not bad['linked']['edges']
 write(root/'current.json',original)
 pinned=copy.deepcopy(manifest);pinned['imports'][0]['sha256']=hashlib.sha256((root/'baseline.json').read_bytes()).hexdigest();write(root/'imports.json',pinned);analyze()
 pinned['imports'][0]['sha256']='0'*64;write(root/'imports.json',pinned);assert analyze(2)['diagnostics'][0]['code']=='digest-mismatch';write(root/'imports.json',manifest)
 # Explicit promotion compares schema-free old/new meaning; schema-selected is stale.
 new=copy.deepcopy(original);new['attributeSchema']=None
 for r in new['requirements']:r['values']['attributes']={};r['locations']['attributes']={}
 old=copy.deepcopy(new);old.pop('attributeSchema');old.update(format='mundanereq-requirements-0.1',sourceContract='mundanereq-yaml-0.3')
 for r in old['requirements']:r['values'].pop('attributes');r['locations'].pop('attributes')
 write(root/'baseline.json',old);write(root/'current.json',new);analyze();write(root/'current.json',original);assert all(r['schemaChanged'] for r in analyze(1)['coverage'])
 forged=copy.deepcopy(old);forged['requirements'][0]['values']['attributes']={};write(root/'current.json',forged);analyze(2)
 # Actual YAML work-item compiler and parser-free analysis consume the same strict requirement boundary.
 work={'format':'mundane-work-yaml-0.2','id':'TC-ATTR','kind':'task','title':'Review logger ownership','status':'Planned','relations':[{'relation':'relates-to','scope':'req','kind':'requirement','target':'SYS-001'}],'body':'Review the descriptive ownership without inferring assessment.\n'};write(root/'task.yaml',work);write(root/'work-set.json',{'format':'mundane-work-set-0.2','source':'mundane-work-yaml-0.2','files':['task.yaml']})
 (root/'work.json').write_bytes(run([str(BIN/'mundane-work')],['compile','--root','.','work-set.json'],root));write(root/'current.json',original);write(root/'work-imports.json',{'format':'mundane-imports-0.1','imports':[{'scope':'req','path':'current.json','kind':'requirements','sha256':None,'dependsOn':[]}]})
 workargs=['analyze','--root','.','--imports','work-imports.json','work.json']
 results=[run(c,workargs,root) for c in commands('mundane-work','engineering.work.WorkMain')];assert results[0]==results[1];assert [e['to'] for e in json.loads(results[0])['edges']]==['req:requirement:SYS-001']
 bad=copy.deepcopy(original);bad['requirements'][0]['values']['attributes']['discipline']='bad';write(root/'current.json',bad)
 for c in commands('mundane-work','engineering.work.WorkMain'):
  result=json.loads(run(c,workargs,root,1));assert not result['complete'] and not result['edges']
print('PASS attribute linking/analysis: serialized mutations, scoped schemas, conservative findings, exact pins, old/new promotion, golden and parser-free JVM/native work links')
