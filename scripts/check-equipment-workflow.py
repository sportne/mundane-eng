"""Native one-source equipment/BOM/wiring regression and imported baseline checks."""
import copy,hashlib,json,shutil,subprocess,tempfile
from pathlib import Path
from ruamel.yaml import YAML
ROOT=Path(__file__).resolve().parents[1];HERE=ROOT/'examples/ground-control-station';yaml=YAML(typ='safe');yaml.allow_duplicate_keys=False

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(root,*args,code=0,command=None):
    p=subprocess.run([*(command or [str(ROOT/'build/maintained/mundane-equipment')]),args[0],'--root',str(root),*args[1:]],capture_output=True)
    assert p.returncode==code,(args,p.returncode,p.stderr.decode());return p.stdout

def stage(root):
    shutil.copytree(ROOT/'build/gcs-configuration',root,dirs_exist_ok=True)
    shutil.copy2(HERE/'design/equipment.yaml',root/'equipment.yaml')
    shutil.copy2(HERE/'design/resources/equipment-datasheet.txt',root/'resources/equipment-datasheet.txt')
    imports=[dict(scope=scope,kind=kind,format=json.loads((root/file).read_text())['format'],path=file,sha256=sha(root/file)) for scope,kind,file in [('gcs-config','configuration','configuration-sim.json'),('gcs-arch','architecture','architecture.json')]]
    (root/'equipment-imports.json').write_text(json.dumps(dict(format='mundane-domain-imports-0.1',imports=imports)))
    compiled=run(root,'compile','--imports','equipment-imports.json','equipment.yaml');(root/'equipment.json').write_bytes(compiled);return compiled

def verify():
    out=ROOT/'build/gcs-equipment'
    if out.exists():shutil.rmtree(out)
    compiled=stage(out)
    for view in ['view','bom','wiring']:(out/('equipment-'+view+'.md')).write_bytes(run(out,view,'equipment.json'))
    assert not json.loads(run(out,'analyze','equipment.json'))['findings']
    cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','equipment'],cwd=ROOT,text=True).strip();cp=':'.join(p for p in cp.split(':') if '/domain-source/' not in p and '/yaml/' not in p and not p.endswith('.jar'))
    assert run(out,'check','equipment.json',command=['java','-cp',cp,'engineering.equipment.EquipmentMain'])==compiled
    with tempfile.TemporaryDirectory(prefix='equipment-rebuild-') as tmp:
        root=Path(tmp);assert stage(root)==compiled;source=yaml.load(root/'equipment.yaml')
        for case in ['voltage','connector','crossed-pin','duplicate-feed','disconnected','ground','substitute','installation']:
            d=copy.deepcopy(source)
            if case=='voltage':d['parts'][3]['ports'][0]['maxV']=12
            if case=='connector':d['parts'][3]['ports'][0]['connector']='unknown'
            if case=='crossed-pin':d['cables'][0]['conductors'][0]['toPin']='R'
            if case=='duplicate-feed':d['cables'].append(dict(d['cables'][0],id='OTHER'))
            if case=='disconnected':del d['cables'][1]
            if case=='ground':d['cables']=[c for c in d['cables'] if c['id']!='CAB-E-HOST']
            if case=='installation':d['basis']='synthetic-installation'
            if case=='substitute':
                d['parts'][3]['id']='PART-HOST-2';d['parts'][3]['model']='SUBSTITUTE';d['parts'][3]['reviewedEvidenceSha256']='0'*64
                d['instances'][3]['part']='PART-HOST-2';d['substitutions']=[dict(instance='EQ-HOST',previousPart='PART-HOST',reason='Illustrative replacement')]
            yaml.dump(d,root/'changed.yaml')
            rejected=case in ['voltage','connector','crossed-pin','duplicate-feed','installation']
            data=run(root,'compile','--imports','equipment-imports.json','changed.yaml',code=1 if rejected else 0)
            if not rejected:
                (root/'changed.json').write_bytes(data);findings=json.loads(run(root,'analyze','changed.json'))['findings']
                expected={'disconnected':'unprotected-power-path','ground':'ground-reference-unavailable','substitute':'stale-datasheet-review'}[case]
                assert expected in {f['code'] for f in findings}
                if case=='substitute':
                    assert b'PART-HOST-2' in run(root,'bom','changed.json')
                    assert b'changed.yaml#L' in run(root,'wiring','changed.json')
        (root/'equipment.yaml').unlink();assert run(root,'check','equipment.json')==compiled
        assert b'source unavailable' in run(root,'wiring','equipment.json')
        (root/'resources/equipment-datasheet.txt').write_text('changed rating\n');run(root,'check','equipment.json',code=1)
    print('PASS native equipment: one-source BOM/wiring, eight substitutions/faults, grounded/protected paths, source navigation, clean rebuild and YAML-free consumers')
if __name__=='__main__':verify()
