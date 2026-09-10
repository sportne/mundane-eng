"""GCS review questions answered from the same pinned inputs as owning CLIs."""
import importlib.util,json,shutil,tempfile,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('gcs_assurance',ROOT/'scripts/check-assurance-workflow.py');a=importlib.util.module_from_spec(s);s.loader.exec_module(a)
def section(report,kind):return next(s for s in report['sections'] if s['kind']==kind)
def review(root,command='analyze',inventory='change.json',baseline='all',scenario='all',code=0,consumer=None):
 return a.run(root,'review',command,inventory,a.AT,'trust.jwks.json',baseline,scenario,code=code,command=consumer)
def verify():
 out=ROOT/'build/gcs-review'
 if out.exists():shutil.rmtree(out)
 shutil.copytree(ROOT/'build/gcs-change',out)
 report=json.loads(review(out));a.emit(out/'review.json',report)
 markdown=review(out,'view');(out/'review.md').write_bytes(markdown)
 assert report['complete'] and report['authorization']=='none'
 control=next(r for r in section(report,'safety')['records'] if r['id']=='CTRL-FRESH');assert control['facts']['owner']=='software' and b'CTRL-FRESH' in markdown
 equipment=section(report,'equipment');assert any(r['group']=='cables' for r in equipment['records'])
 budget=section(report,'budget');assert budget['analysis']==json.loads(a.run(out,'budget','analyze','budget.json'))
 operations=section(report,'operations')['analysis'];cli=json.loads(a.run(out,'operations','analyze','operations.json',a.AT,'trust.jwks.json'));cli.pop('trustSetSha256');assert operations==cli
 blocked=[c for c in operations['candidates'] if not c['localReadiness']];assert blocked and all(c['blockers'] for c in blocked)
 for c in operations['candidates']:assert c['buildSha256'].encode() in markdown and c['id'].encode() in markdown
 initial=next(r['facts'] for r in section(report,'operations')['records'] if r['id']=='EXEC-STARTUP')
 candidate=next(c for c in operations['candidates'] if c['id']==initial['candidate'])
 assert candidate['buildSha256']==initial['observedBuildSha256']
 selected_run=next(s for s in report['sections'] if s['scope']==initial['evidenceScopes'][0])
 run=next(r['facts'] for r in selected_run['records'] if r['group']=='run')
 assert run['id']=='RUN-PROC-COMBINED-nominal' and run['id'].encode() in markdown
 declarations=next(r for r in section(report,'requirements')['records'] if r['group']=='declarations');assert declarations['origin']['source']['path']=='source/attributes.yaml'
 activity=next(r for r in section(report,'verification-plan')['records'] if r['id']=='ACT-STATE')
 compiled_plan=json.loads((out/'plan.json').read_text());expected=next(r for r in compiled_plan['activities'] if r['id']=='ACT-STATE')['location']
 assert activity['origin']['location']==expected
 assert report['unresolved']
 filtered=json.loads(review(out,baseline='configuration-software',scenario='PLAN-STARTUP'))
 ops=section(filtered,'operations');executions=[r['facts'] for r in ops['records'] if r['group']=='executions']
 assert executions and all(e['plan']=='PLAN-STARTUP' for e in executions)
 assert not any(s['kind']=='budget' for s in filtered['sections'])
 assert [s['scope'] for s in filtered['sections'] if s['kind']=='configuration']==['configuration-software']
 simulation=json.loads(review(out,baseline='configuration-sim'))
 assert [s['scope'] for s in simulation['sections'] if s['kind']=='configuration']==['configuration-sim']
 assert not any(s['kind']=='evidence' for s in simulation['sections'])
 a.emit(out/'startup-review.json',filtered)
 # Actual host-part substitution, retaining the old budget and review selections.
 equipment_source=a.yaml.load(out/'equipment.yaml')
 host=next(p for p in equipment_source['parts'] if p['id']=='PART-HOST')
 host['id']='PART-HOST-2';host['model']='SUBSTITUTED-HOST';host['reviewedEvidenceSha256']='0'*64
 next(r for r in host['ratings'] if r['id']=='PEAK')['value']=200
 next(i for i in equipment_source['instances'] if i['id']=='EQ-HOST')['part']='PART-HOST-2'
 equipment_source['substitutions']=[dict(instance='EQ-HOST',previousPart='PART-HOST',reason='Illustrative 200 W host substitution; review is outstanding.')]
 a.yaml.dump(equipment_source,out/'replacement.yaml')
 (out/'replacement.json').write_bytes(a.run(out,'equipment','compile','--imports','equipment-imports.json','replacement.yaml'))
 inventory=a.yaml.load(out/'change.yaml')
 inventory['after']=[a.selection(out,'equipment','equipment','replacement.json') if e['scope']=='equipment' else e for e in inventory['after']]
 a.yaml.dump(inventory,out/'change-substitution.yaml')
 (out/'change-substitution.json').write_bytes(a.run(out,'change','compile','change-substitution.yaml'))
 substitution=json.loads(review(out,inventory='change-substitution.json'));assert section(substitution,'budget')['state']=='stale'
 a.emit(out/'substitution-review.json',substitution)
 updated_budget=a.yaml.load(out/'budget.yaml')
 quantity=next(p for p in updated_budget['parameters'] if p['id']=='HOST-PEAK');quantity['min']=quantity['max']=200
 imports=json.loads((out/'budget-imports.json').read_text());imports['imports']=[a.selection(out,'gcs-equipment','equipment','replacement.json') if e['scope']=='gcs-equipment' else e for e in imports['imports']]
 a.emit(out/'replacement-budget-imports.json',imports);a.yaml.dump(updated_budget,out/'replacement-budget.yaml')
 (out/'replacement-budget.json').write_bytes(a.run(out,'budget','compile','--imports','replacement-budget-imports.json','replacement-budget.yaml'))
 inventory['after']=[a.selection(out,'budget','budget','replacement-budget.json') if e['scope']=='budget' else e for e in inventory['after']]
 a.yaml.dump(inventory,out/'change-rebudget.yaml')
 (out/'change-rebudget.json').write_bytes(a.run(out,'change','compile','change-rebudget.yaml'))
 rebudget=json.loads(review(out,inventory='change-rebudget.json'))
 assert {r['id'] for r in section(rebudget,'budget')['analysis']['checks'] if r['state']=='fail'}=={'CHECK-POWER','CHECK-RUNTIME'}
 assert 'equipment-review-stale' in {r['code'] for r in section(rebudget,'budget')['analysis']['findings']}
 a.emit(out/'rebudget-review.json',rebudget)
 unknown=json.loads(review(out,inventory='change-unknown.json'));assert not unknown['complete'] and any(s['state']=='unknown' for s in unknown['sections'])
 review(out,baseline='missing',code=1);review(out,scenario='missing',code=1)
 cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','review'],text=True).strip();assert not any(x in cp for x in ['/yaml/','domain-source','snakeyaml'])
 assert json.loads(review(out,consumer=['java','-cp',cp,'engineering.review.ReviewMain']))==report
 with tempfile.TemporaryDirectory(prefix='gcs-review-rebuild-') as temp:
  root=Path(temp);shutil.copytree(out,root,dirs_exist_ok=True)
  assert json.loads(review(root))==report
  # Drop human engineering origins; retained native sources explicitly selected as
  # configuration members remain required resources, not parser dependencies.
  for file in ['safety-software.yaml','procedure-stale.yaml','equipment.yaml','budget.yaml','operations.yaml','waived.yaml','change.yaml']:(root/file).unlink()
  assert json.loads(review(root))==report
  assert b'source revision unavailable' in review(root,'view')
  (root/'bom.cdx.json').unlink();review(root,code=2)
 print('PASS native engineering review: control ownership, exact build/run support, blocked release reasons, equipment/budget change invalidation, baseline/scenario filters, unknown states, CLI/render parity, retained-root reproduction and unavailable source navigation')
if __name__=='__main__':verify()
