"""Native GCS assurance: independently signed reviews, retained runs and explicit expiry."""
import copy,hashlib,json,shutil,subprocess,tempfile
from pathlib import Path
from ruamel.yaml import YAML
ROOT=Path(__file__).resolve().parents[1];HERE=ROOT/'examples/ground-control-station';BIN=ROOT/'build/maintained';yaml=YAML(typ='safe');yaml.default_flow_style=False
yaml.allow_duplicate_keys=False
AT='2026-09-08T12:00:00Z'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def emit(p,d):p.write_text(json.dumps(d,sort_keys=True,indent=2)+'\n')
def run(root,tool,*args,code=0,command=None):
    p=subprocess.run([*(command or [str(BIN/('mundane-'+tool))]),args[0],'--root',str(root),*args[1:]],capture_output=True)
    assert p.returncode==code,(tool,args,p.returncode,p.stderr.decode());return p.stdout

def selection(root,scope,kind,file):return dict(scope=scope,kind=kind,format=json.loads((root/file).read_text())['format'],path=file,sha256=sha(root/file))
def compile_case(root,source,name='assurance',imports='assurance-imports.json'):
    yaml.dump(source,root/(name+'.yaml'));data=run(root,'assurance','compile','--imports',imports,name+'.yaml');(root/(name+'.json')).write_bytes(data);return data

def analyze(root,name='assurance',at=AT,trust='trust.jwks.json'):
    return json.loads(run(root,'assurance','analyze',name+'.json',at,trust))

def sign(root,keydir,record,name):
    emit(keydir/'payload.json',record)
    subprocess.run(['node',str(HERE/'design/review-fixture.mjs'),'sign',str(keydir),str(keydir/'payload.json'),str(root/(name+'.dsse.json'))],check=True)
    return dict(record,signature=dict(path=name+'.dsse.json',sha256=sha(root/(name+'.dsse.json'))))

def stage(root):
    shutil.copytree(ROOT/'build/gcs-software',root,dirs_exist_ok=True)
    shutil.copy2(ROOT/'build/gcs-seed/plan.json',root/'plan.json')
    entries=[selection(root,*row) for row in [('gcs-req','requirements','requirements.json'),('gcs-arch','architecture','architecture.json'),('gcs-config','configuration','configuration-software.json'),('gcs-plan','verification-plan','plan.json')]]
    emit(root/'procedure-imports.json',dict(format='mundane-domain-imports-0.1',imports=entries))
    (root/'runtime').mkdir(exist_ok=True);shutil.copy2(BIN/'mundane-evidence',root/'runtime/mundane-evidence')
    for name in ['stale','combined']:
        p=yaml.load(HERE/'design'/('procedure-'+name+'.yaml'));p['configuration']['id']='BL-SOFTWARE-DEPLOYED';yaml.dump(p,root/('procedure-'+name+'.yaml'))
        (root/('procedure-'+name+'.json')).write_bytes(run(root,'procedure','compile','--imports','procedure-imports.json','procedure-'+name+'.yaml'))
        (root/('run-'+name+'.json')).write_bytes(run(root,'evidence','simulate','procedure-'+name+'.json','runtime/mundane-evidence','nominal'))
        (root/('evidence-'+name+'.json')).write_bytes(run(root,'evidence','import','procedure-'+name+'.json','run-'+name+'.json'))
        entries.extend([selection(root,'proc-'+name,'procedure','procedure-'+name+'.json'),selection(root,'evidence-'+name,'evidence','evidence-'+name+'.json')])
    entries.extend([selection(root,'gcs-safety','safety','safety-software.json'),selection(root,'gcs-software','software','software.json')])
    emit(root/'assurance-imports.json',dict(format='mundane-domain-imports-0.1',imports=entries))
    source=yaml.load(HERE/'design/assurance.yaml');compile_case(root,source)
    subjects=json.loads(run(root,'assurance','subjects','assurance.json'))
    with tempfile.TemporaryDirectory(prefix='gcs-ephemeral-review-key-') as tmp:
        keydir=Path(tmp);subprocess.run(['node',str(HERE/'design/review-fixture.mjs'),'keygen',tmp],check=True)
        shutil.copy2(keydir/'trust.jwks.json',root/'trust.jwks.json')
        base=dict(**subjects,reviewer='fixture-reviewer',role=source['requiredRole'],issuedAt='2026-09-07T00:00:00Z',expiresAt='2026-10-01T00:00:00Z',reason='Synthetic scoped example; not organizational release approval.')
        source['reviews']=[sign(root,keydir,dict(base,id='REV-'+c['id'],claim=c['id'],decision='adequate'),'review-'+c['id']) for c in source['claims']]
        compile_case(root,source)
        waived=copy.deepcopy(source);waived['waivers']=[sign(root,keydir,dict(base,id='WAIVE-FIELD',obligation='OBL-FIELD'),'waive-field')];compile_case(root,waived,'waived')
        disputed=copy.deepcopy(waived);disputed['reviews'].append(sign(root,keydir,dict(base,id='REV-DISPUTE',claim='CLAIM-STATE',decision='disputed'),'review-dispute'));compile_case(root,disputed,'disputed')
    return source,waived

def verify():
    out=ROOT/'build/gcs-assurance'
    if out.exists():shutil.rmtree(out)
    source,waived=stage(out)
    r=analyze(out);assert not r['localReadiness'] and r['obligations'][0]['state']=='open' and all(c['state']=='supported' and c['reviewAdequate'] for c in r['claims']),r
    r=analyze(out,'waived');assert r['localReadiness'] and r['authorization']=='none' and r['obligations'][0]['state']=='waived',r
    r=analyze(out,'disputed');assert not r['localReadiness'] and any(c['state']=='disputed' for c in r['claims'])
    assert not analyze(out,'waived',at='2026-10-01T00:00:00Z')['localReadiness']
    (out/'assurance.md').write_bytes(run(out,'assurance','view','assurance.json',AT,'trust.jwks.json'))
    emit(out/'analysis.json',analyze(out));emit(out/'waived-analysis.json',analyze(out,'waived'))
    cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','assurance'],cwd=ROOT,text=True).strip()
    cp=':'.join(p for p in cp.split(':') if '/domain-source/' not in p and '/yaml/' not in p and not p.endswith('.jar'))
    assert run(out,'assurance','check','assurance.json',command=['java','-cp',cp,'engineering.assurance.AssuranceMain'])==(out/'assurance.json').read_bytes()
    with tempfile.TemporaryDirectory(prefix='assurance-rebuild-') as tmp:
        root=Path(tmp);shutil.copytree(out,root,dirs_exist_ok=True)
        # Reuse exact selected native signature bytes, never regenerate a random identity.
        assert compile_case(root,source)==(out/'assurance.json').read_bytes()
        emit(root/'empty.jwks.json',dict(keys=[]));assert not analyze(root,'waived',trust='empty.jwks.json')['localReadiness']
        d=copy.deepcopy(waived);d['claims'][0]['reasoning']+=' Changed reasoning.';compile_case(root,d,'changed');r=analyze(root,'changed');assert not r['localReadiness'] and all(x['state']=='stale' for x in r['reviews'])
        d=copy.deepcopy(waived);d['reviews'][0]['signature']=None;compile_case(root,d,'changed');r=analyze(root,'changed');assert not r['localReadiness'] and r['reviews'][0]['state']=='identity-unverified'
        d=copy.deepcopy(waived);d['reviews'][0]['reason']+=' Not the signed payload.';compile_case(root,d,'changed');assert not analyze(root,'changed')['localReadiness']
        d=copy.deepcopy(waived);d['claims'][1]['evidenceScopes']=[];compile_case(root,d,'changed');assert any(c['state']=='unsupported' for c in analyze(root,'changed')['claims'])
        (root/'run-defect.json').write_bytes(run(root,'evidence','simulate','procedure-stale.json','runtime/mundane-evidence','suppress-stale'))
        (root/'evidence-defect.json').write_bytes(run(root,'evidence','import','procedure-stale.json','run-defect.json'))
        imports=json.loads((root/'assurance-imports.json').read_text());imports['imports'].append(selection(root,'evidence-defect','evidence','evidence-defect.json'));emit(root/'changed-imports.json',imports)
        d=copy.deepcopy(waived);d['claims'][1]['evidenceScopes'].append('evidence-defect');compile_case(root,d,'changed','changed-imports.json');r=analyze(root,'changed');assert any(c['state']=='disputed' for c in r['claims']) and not r['localReadiness']
        p=yaml.load(root/'procedure-stale.yaml');p['objective']+=' New revision.';yaml.dump(p,root/'new-procedure.yaml');(root/'new-procedure.json').write_bytes(run(root,'procedure','compile','--imports','procedure-imports.json','new-procedure.yaml'))
        imports=json.loads((root/'assurance-imports.json').read_text());imports['imports']=[selection(root,'proc-stale','procedure','new-procedure.json') if e['scope']=='proc-stale' else e for e in imports['imports']];emit(root/'changed-imports.json',imports)
        compile_case(root,waived,'changed','changed-imports.json');assert any(c['observedSupport']=='stale' for c in analyze(root,'changed')['claims'])
        for kind in ['cycle','wrong-config']:
            d=copy.deepcopy(source)
            if kind=='cycle':d['claims'][1]['children']=['CLAIM-GCS']
            else:d['configuration']['id']='BL-SIM-DESIGN'
            yaml.dump(d,root/'invalid.yaml');run(root,'assurance','compile','--imports','assurance-imports.json','invalid.yaml',code=1)
        # Signature mismatch with a correct resource digest is unverified, not a parse success.
        native=json.loads((root/'review-CLAIM-GCS.dsse.json').read_text());native['signatures'][0]['sig']='AA=='
        emit(root/'invalid.dsse.json',native);d=copy.deepcopy(waived);d['reviews'][0]['signature']=dict(path='invalid.dsse.json',sha256=sha(root/'invalid.dsse.json'));compile_case(root,d,'changed');assert not analyze(root,'changed')['localReadiness']
        (root/'run-stale.json').write_bytes((root/'run-stale.json').read_bytes()+b' ');run(root,'assurance','check','assurance.json',code=1)
        (root/'run-stale.json').unlink();run(root,'assurance','check','assurance.json',code=2)
    print('PASS native assurance: GCS exact-config runs, independent DSSE/JWK verification, open/waived obligations, conflicts/cycles, changed subjects/evidence, expired review/waiver, identity rejection, native pin failures, clean rebuild and YAML-free consumer')
if __name__=='__main__':verify()
