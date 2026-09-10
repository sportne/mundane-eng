"""Real GCS mutations over retained compiled revisions; no source readers in the consumer."""
import copy,importlib.util,json,shutil,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('gcs_assurance',ROOT/'scripts/check-assurance-workflow.py');a=importlib.util.module_from_spec(s);s.loader.exec_module(a)
yaml=a.yaml;run=a.run;sha=a.sha;emit=a.emit;selection=a.selection
FILES=[('requirements','requirements.json'),('verification-plan','plan.json'),('architecture','architecture.json'),*[( 'configuration','configuration-'+n+'.json') for n in ['sim','field','built','deployed','replacement','software']],('safety','safety-software.json'),('software','software.json'),('procedure','procedure-stale.json'),('procedure','procedure-combined.json'),('procedure','procedure-corrected.json'),('evidence','evidence-stale.json'),('evidence','evidence-combined.json'),('evidence','evidence-corrected.json'),('assurance','waived.json'),('operations','operations.json'),('work-items','corrective-work.json'),('equipment','equipment.json'),('budget','budget.json')]
def inventory(root):return [selection(root,Path(file).stem,kind,file) for kind,file in FILES]
def compile_change(root,d,name='change',code=0):
 yaml.dump(d,root/(name+'.yaml'));data=run(root,'change','compile',name+'.yaml',code=code)
 if code==0:(root/(name+'.json')).write_bytes(data)
 return data
def node(result,scope):return next(n for n in result['nodes'] if n['scope']==scope)
def stage(root):
 shutil.copytree(ROOT/'build/gcs-operations',root,dirs_exist_ok=True)
 # The equipment/budget example uses the simulation baseline, while operations use the deployed one.
 shutil.copytree(ROOT/'build/gcs-budget',root,dirs_exist_ok=True)
 entries=inventory(root);d=dict(format='mundane-change-yaml-0.1',id='CHANGE-GCS',before=copy.deepcopy(entries),after=copy.deepcopy(entries))
 compile_change(root,d);return d
def mutate(root,case):
 if case=='telemetry':
  original=(root/'source/requirements.yaml').read_text();assert '500 ms' in original
  (root/'changed-requirements.yaml').write_text(original.replace('500 ms','400 ms'))
  p=subprocess.run([str(a.BIN/'mundanereq-compile'),'--source=yaml-0.4','--root',str(root),'--attribute-schema',str(root/'source/attributes.yaml'),str(root/'changed-requirements.yaml')],capture_output=True);assert p.returncode==0,p.stderr
  (root/'changed-requirements.json').write_bytes(p.stdout);return 'requirements','requirements','changed-requirements.json'
 if case=='unknown':
  emit(root/'future.json',dict(artifactKind='future-domain',format='future-1',values=dict(newMeaning=True)));return 'future','future-domain','future.json'
 scope,tool,file,imports={'server-power':('budget','budget','budget.yaml','budget-imports.json'),'cable':('equipment','equipment','equipment.yaml','equipment-imports.json'),'binary-dependency':('software','software','software.yaml','software-imports.json'),'procedure':('procedure-stale','procedure','procedure-stale.yaml','procedure-imports.json'),'formatting':('procedure-stale','procedure','procedure-stale.yaml','procedure-imports.json'),'reviewer':('waived','assurance','waived.yaml','assurance-imports.json')}[case]
 d=yaml.load(root/file)
 if case=='server-power':next(p for p in d['parameters'] if p['id']=='HOST-PEAK')['max']=120
 elif case=='cable':d['cables']=[c for c in d['cables'] if c['id']!='CAB-E-HOST']
 elif case=='binary-dependency':
  for source,key in [('bom.cdx.json','bom'),('scan.cdx.json','scan')]:
   native=json.loads((root/source).read_text());native['components'][0]['version']='2.0';emit(root/('changed-'+source),native)
  d['build']['sbom']=dict(path='changed-bom.cdx.json',sha256=sha(root/'changed-bom.cdx.json'));d['scan']['result']=dict(path='changed-scan.cdx.json',sha256=sha(root/'changed-scan.cdx.json'))
 elif case=='procedure':d['expected'][0]['withinMs']+=1
 elif case=='reviewer':d['reviews'][0]['decision']='disputed' # deliberately no re-sign: preserve the invalidated old signature
 new='changed-'+case+'-'+file
 if case=='formatting':(root/new).write_text('# presentation only\n'+(root/file).read_text())
 else:yaml.dump(d,root/new)
 output=str(Path(new).with_suffix('.json'));(root/output).write_bytes(run(root,tool,'compile','--imports',imports,new));return scope,tool,output
def verify():
 out=ROOT/'build/gcs-change'
 if out.exists():shutil.rmtree(out)
 baseline=stage(out);plain=json.loads(run(out,'change','analyze','change.json'));assert plain['complete'],plain['unresolved'];assert all(n['change']=='unchanged' for n in plain['nodes'])
 expected=yaml.load(ROOT/'examples/ground-control-station/design/change-cases.yaml')['cases'];ledger=[]
 for case in expected:
  scope,kind,file=mutate(out,case['id']);d=copy.deepcopy(baseline);entry=selection(out,scope,kind,file)
  if case['id']=='unknown':d['after'].append(entry)
  else:d['after']=[entry if i['scope']==scope else i for i in d['after']]
  name='change-'+case['id'];compile_change(out,d,name);r=json.loads(run(out,'change','query',name+'.json',scope,'16'));n=node(r,scope)
  if case['id'] in ['formatting','unknown']:assert n['change']==case['classification'],(case,n)
  else:assert n['change']=='semantic-change' and case['classification'] in {x['classification'] for x in n['differences']},(case,n)
  if case['id'] in ['formatting','procedure']:
   assert node(r,'evidence-stale')['supportState']=='stale'
   assert {'evidence-stale','waived','operations'} <= {p['target'] for p in r['paths']}
   assert node(r,'equipment')['change']=='unchanged' and node(r,'operations')['supportState']=='stale'
  if case['id']=='telemetry':
   assert node(r,'equipment')['supportState']=='no-direct-staleness' and 'equipment' in {p['target'] for p in r['paths']}
  if case['id']=='reviewer':assert node(r,'operations')['supportState']=='stale'
  if case['id']=='cable':assert node(r,'budget')['supportState']=='stale'
  if case['id']=='unknown':assert not r['complete']
  emit(out/(name+'-analysis.json'),r);ledger.append(dict(case=case['id'],change=n['change'],paths=r['paths'],differences=n['differences']))
 # Re-execute the changed procedure and explicitly select the new observation.
 changed_proc='changed-procedure-procedure-stale.json'
 (out/'run-revised.json').write_bytes(run(out,'evidence','simulate',changed_proc,'runtime/mundane-evidence','nominal'))
 (out/'evidence-revised.json').write_bytes(run(out,'evidence','import',changed_proc,'run-revised.json'))
 revised=copy.deepcopy(baseline)
 replacements={'procedure-stale':selection(out,'procedure-stale','procedure',changed_proc),'evidence-stale':selection(out,'evidence-stale','evidence','evidence-revised.json')}
 revised['after']=[replacements.get(e['scope'],e) for e in revised['after']]
 compile_change(out,revised,'change-reexecuted')
 rebuilt=json.loads(run(out,'change','analyze','change-reexecuted.json'))
 assert node(rebuilt,'evidence-stale')['supportState']=='reassessment-required'
 assert node(rebuilt,'waived')['supportState']=='stale'
 emit(out/'change-reexecuted-analysis.json',rebuilt)
 emit(out/'scenario-ledger.json',ledger);(out/'change.md').write_bytes(run(out,'change','view','change-procedure.json'))
 cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','change'],text=True).strip();cp=':'.join(p for p in cp.split(':') if '/domain-source/' not in p and '/yaml/' not in p and not p.endswith('.jar'))
 assert run(out,'change','check','change.json',command=['java','-cp',cp,'engineering.change.ChangeMain'])==(out/'change.json').read_bytes()
 with tempfile.TemporaryDirectory(prefix='change-rebuild-') as tmp:
  root=Path(tmp);shutil.copytree(out,root,dirs_exist_ok=True);assert compile_change(root,baseline)==(out/'change.json').read_bytes()
  for case in ['duplicate','pin','path']:
   d=copy.deepcopy(baseline)
   if case=='duplicate':d['after'].append(d['after'][0].copy())
   elif case=='pin':d['after'][0]['sha256']='0'*64
   else:d['after'][0]['path']='../outside.json'
   compile_change(root,d,'invalid',1)
  d=copy.deepcopy(baseline);d['after']=[x for x in d['after'] if x['scope']!='procedure-stale'];compile_change(root,d,'incomplete');assert not json.loads(run(root,'change','analyze','incomplete.json'))['complete']
  for p in root.glob('change*.yaml'):p.unlink()
  assert run(root,'change','check','change.json')==(out/'change.json').read_bytes()
  assert b'source unavailable' in run(root,'change','view','change.json')
  (root/'requirements.json').unlink();run(root,'change','check','change.json',code=2)
 print('PASS native change: eight real GCS mutations, selected-pin staleness, independent unaffected artifact, prospective paths, unknown coverage, exact provenance, clean rebuild and YAML-free consumer')
if __name__=='__main__':verify()
