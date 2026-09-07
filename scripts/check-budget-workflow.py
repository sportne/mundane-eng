"""Native dimensional budget and conditional availability reference workflow."""
import copy,hashlib,json,shutil,subprocess,tempfile
from pathlib import Path
from ruamel.yaml import YAML
ROOT=Path(__file__).resolve().parents[1];HERE=ROOT/'examples/ground-control-station';yaml=YAML(typ='safe');yaml.allow_duplicate_keys=False

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def emit(p,d):p.write_text(json.dumps(d,sort_keys=True,indent=2)+'\n')
def run(root,*args,code=0,tool='budget',command=None):
    p=subprocess.run([*(command or [str(ROOT/'build/maintained'/('mundane-'+tool))]),args[0],'--root',str(root),*args[1:]],capture_output=True)
    assert p.returncode==code,(args,p.returncode,p.stderr.decode());return p.stdout

def stage(root):
    shutil.copytree(ROOT/'build/gcs-equipment',root,dirs_exist_ok=True)
    shutil.copy2(HERE/'design/budget.yaml',root/'budget.yaml')
    imports=json.loads((root/'equipment-imports.json').read_text());imports['imports'].append(dict(scope='gcs-equipment',kind='equipment',format='mundane-equipment-0.1',path='equipment.json',sha256=sha(root/'equipment.json')));emit(root/'budget-imports.json',imports)
    compiled=run(root,'compile','--imports','budget-imports.json','budget.yaml');(root/'budget.json').write_bytes(compiled);return compiled

def verify():
    out=ROOT/'build/gcs-budget'
    if out.exists():shutil.rmtree(out)
    compiled=stage(out);result=json.loads(run(out,'calculate','budget.json'))
    (out/'budget.md').write_bytes(run(out,'view','budget.json'));emit(out/'calculation.json',result)
    reference=json.loads((ROOT/'build/gcs-design/budget-reference.json').read_text())
    for key,value in reference['pointValues'].items():
        assert abs(result['values'][key]['min']-value)<1e-10 and abs(result['values'][key]['max']-value)<1e-10
    assert abs(result['reliability'][0]['availability']['min']-reference['availability'])<1e-12
    assert not result['findings'] and all(c['state']=='pass' for c in result['checks'])
    cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','budget'],cwd=ROOT,text=True).strip();cp=':'.join(p for p in cp.split(':') if '/domain-source/' not in p and '/yaml/' not in p and not p.endswith('.jar'))
    assert run(out,'check','budget.json',command=['java','-cp',cp,'engineering.budget.BudgetMain'])==compiled
    with tempfile.TemporaryDirectory(prefix='budget-rebuild-') as tmp:
        root=Path(tmp);assert stage(root)==compiled;source=yaml.load(root/'budget.yaml')
        def changed(d,selection='budget-imports.json',code=0):
            yaml.dump(d,root/'changed.yaml');data=run(root,'compile','--imports',selection,'changed.yaml',code=code)
            if code:return None
            (root/'changed.json').write_bytes(data);return json.loads(run(root,'calculate','changed.json'))
        def param(d,id):return next(p for p in d['parameters'] if p['id']==id)
        for case in ['mixed','missing','cycle','zero','no-evidence','reversed','fraction']:
            d=copy.deepcopy(source)
            if case=='mixed':param(d,'HOST-PEAK')['unit']='Wh'
            if case=='missing':d['formulas'][0]['inputs'][0]='ABSENT'
            if case=='cycle':d['formulas'][0]['inputs'][0]='RUNTIME'
            if case=='zero':param(d,'POWER-LIMIT')['min']=0;d['formulas'][2]['inputs'][1]='POWER-LIMIT'
            if case=='no-evidence':param(d,'NETWORK')['basis']='measurement'
            if case=='fraction':param(d,'EFFICIENCY')['max']=1.1
            if case=='reversed':param(d,'NETWORK')['max']=0
            changed(d,code=1)
        d=copy.deepcopy(source);d['formulas'][0]['inputs'].remove('HOST-PEAK');r=changed(d)
        assert r['checks'][0]['state']=='unavailable' and 'missing-peak-load' in {f['code'] for f in r['findings']}
        d=copy.deepcopy(source);param(d,'HOST-PEAK')['max']=120;r=changed(d);assert r['checks'][0]['state']=='indeterminate'
        d=copy.deepcopy(source);p=param(d,'HOST-PEAK');p['min']=p['max']=.06;p['unit']='kW';r=changed(d);assert r['values']['TOTAL-POWER']['min']==90
        d=copy.deepcopy(source);d['reliability'][0]['independence']='unknown';assert changed(d)['reliability'][0]['availability'] is None
        d=copy.deepcopy(source);param(d,'POWER-REPAIR')['min']=param(d,'POWER-REPAIR')['max']=20;r=changed(d);assert abs(r['reliability'][0]['availability']['min']-reference['delayedRepairAvailability'])<1e-12
        d=copy.deepcopy(source);param(d,'EFFICIENCY')['min']=param(d,'EFFICIENCY')['max']=.8;changed(d)
        comparison=json.loads(run(root,'compare','budget.json','changed.json'));assert comparison['previousResultState']=='stale-for-new-inputs' and comparison['inputRevisionChanged']
        # Explicitly replace equipment, keeping the old reviewed revision to expose staleness.
        eq=yaml.load(root/'equipment.yaml');next(r for r in eq['parts'][3]['ratings'] if r['id']=='PEAK')['value']=200
        eq['parts'][3]['model']='SUBSTITUTED-HOST';eq['parts'][3]['reviewedEvidenceSha256']='0'*64
        yaml.dump(eq,root/'replacement.yaml');(root/'replacement.json').write_bytes(run(root,'compile','--imports','equipment-imports.json','replacement.yaml',tool='equipment'))
        imports=json.loads((root/'budget-imports.json').read_text());entry=next(e for e in imports['imports'] if e['scope']=='gcs-equipment');entry['path']='replacement.json';entry['sha256']=sha(root/'replacement.json');emit(root/'replacement-imports.json',imports)
        d=copy.deepcopy(source);param(d,'HOST-PEAK')['min']=param(d,'HOST-PEAK')['max']=200;r=changed(d,'replacement-imports.json')
        assert 'equipment-review-stale' in {f['code'] for f in r['findings']}
        assert {c['id'] for c in r['checks'] if c['state']=='fail'}=={'CHECK-POWER','CHECK-RUNTIME'}
        (root/'budget.yaml').unlink();assert run(root,'check','budget.json')==compiled
        (root/'equipment.json').write_bytes((root/'equipment.json').read_bytes()+b' ');run(root,'check','budget.json',code=1)
    print('PASS native budget: independent reference values, interval margins, unit conversion, seven rejected models, missing peaks, repair sensitivity, substitution failures, staleness, clean rebuild and YAML-free consumers')
if __name__=='__main__':verify()
