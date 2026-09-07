"""Real native standards adapters, exact deployed selection and adversarial offline cases."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
from ruamel.yaml import YAML
ROOT=Path(__file__).resolve().parents[1];HERE=ROOT/'examples/ground-control-station'
YAML_IO=YAML(typ='safe');YAML_IO.allow_duplicate_keys=False

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
fixture=module('software_fixture',HERE/'design/software-fixture.py')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def emit(p,d):p.write_text(json.dumps(d,sort_keys=True,indent=2)+'\n')
def run(root,tool,*args,code=0,command=None):
    p=subprocess.run([*(command or [str(ROOT/'build/maintained'/('mundane-'+tool))]),args[0],'--root',str(root),*args[1:]],capture_output=True)
    assert p.returncode==code,(tool,args,p.returncode,p.stderr.decode());return p.stdout

def selection(root,scope,kind,file):return dict(scope=scope,kind=kind,format=json.loads((root/file).read_text())['format'],path=file,sha256=sha(root/file))
def stage(root):
    shutil.copytree(ROOT/'build/gcs-safety',root,dirs_exist_ok=True)
    build=fixture.build(root)
    d=YAML_IO.load(root/'configuration-deployed.yaml')
    d['baseline']['id']='BL-SOFTWARE-DEPLOYED';d['configuration']['id']='CFG-SOFTWARE-DEPLOYED'
    d['baseline']['members'].append(dict(scope='software-binary',kind='native-resource',format='application/zip',required=True,appliesTo=['simulation'],**build['binary']))
    d['configuration']['selections'].append(dict(slot='software-binary',memberScope='software-binary'))
    YAML_IO.dump(d,root/'configuration-software.yaml')
    (root/'configuration-software.json').write_bytes(run(root,'configuration','compile','configuration-software.yaml'))
    imports=json.loads((root/'safety-imports.json').read_text())
    imports['imports']=[selection(root,'gcs-config','configuration','configuration-software.json') if e['scope']=='gcs-config' else e for e in imports['imports']]
    emit(root/'safety-software-imports.json',imports)
    safety=YAML_IO.load(root/'safety.yaml');safety['context']['configuration']['id']='BL-SOFTWARE-DEPLOYED'
    YAML_IO.dump(safety,root/'safety-software.yaml')
    (root/'safety-software.json').write_bytes(run(root,'safety','compile','--imports','safety-software-imports.json','safety-software.yaml'))
    imports['imports'].append(selection(root,'gcs-safety','safety','safety-software.json'));emit(root/'software-imports.json',imports)
    source=YAML_IO.load(HERE/'design/software.yaml')
    # The producer deliberately authors a new fixture selection for the built output.
    # It never rewrites checked-in resources or silently repairs a reviewed input.
    assert build==source['build'],'checked-in native fixture identity drift'
    source['configuration']['id']='BL-SOFTWARE-DEPLOYED'
    source['reviews']=[dict(id='REV-REPLAY',advisory='EXAMPLE-REPLAY-001',sbomSha256=sha(root/'bom.cdx.json'),scanSha256=sha(root/'scan.cdx.json'),configurationSha256=sha(root/'configuration-software.json'),reviewer='synthetic-reviewer',rationale='Exercise a revision-bound review; no acceptance decision.')]
    YAML_IO.dump(source,root/'software.yaml')
    compiled=run(root,'software','compile','--imports','software-imports.json','software.yaml');(root/'software.json').write_bytes(compiled)
    return compiled,source

def verify():
    out=ROOT/'build/gcs-software'
    if out.exists():shutil.rmtree(out)
    compiled,source=stage(out)
    (out/'software.md').write_bytes(run(out,'software','view','software.json'))
    query=json.loads(run(out,'software','query','software.json','EXAMPLE-REPLAY-001'))
    assert query['stage']=='deployed' and query['findings'][0]['component']=='link-fixture' and query['controls'][0]['safetyControl']['id']=='CTRL-FRESH'
    assert query['reviews'][0]['state']=='current-authored-review' and query['authorization']=='none'
    cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','software'],cwd=ROOT,text=True).strip()
    cp=':'.join(p for p in cp.split(':') if '/domain-source/' not in p and '/yaml/' not in p and not p.endswith('.jar'))
    assert run(out,'software','check','software.json',command=['java','-cp',cp,'engineering.software.SoftwareMain'])==compiled
    with tempfile.TemporaryDirectory(prefix='software-rebuild-') as tmp:
        root=Path(tmp);assert stage(root)[0]==compiled
        def compile_source(d):
            YAML_IO.dump(d,root/'changed.yaml');data=run(root,'software','compile','--imports','software-imports.json','changed.yaml');(root/'changed.json').write_bytes(data)
            return json.loads(run(root,'software','analyze','changed.json'))
        for state in ['no-scan','failed']:
            d=copy.deepcopy(source);d['scan']['state']=state;d['scan']['result']=None
            result=compile_source(d);assert result['scanState']==state and not result['findings'] and result['reviews'][0]['state']=='stale-or-unavailable-review'
        scan=json.loads((root/'scan.cdx.json').read_text());scan['vulnerabilities']=[];emit(root/'clean.cdx.json',scan)
        d=copy.deepcopy(source);d['scan']['result']=dict(path='clean.cdx.json',sha256=sha(root/'clean.cdx.json'))
        result=compile_source(d);assert result['scanState']=='complete' and not result['findings'] and result['reviews'][0]['state']=='stale-or-unavailable-review'
        d=copy.deepcopy(source);d['reviews'][0]['sbomSha256']='0'*64
        assert compile_source(d)['reviews'][0]['state']=='stale-or-unavailable-review'
        for case in ['missing-source','unsupported-slsa','unsupported-bom','unknown-target','changed-dependency']:
            d=copy.deepcopy(source)
            if case in ['missing-source','unsupported-slsa']:
                raw=json.loads((root/'provenance.intoto.json').read_text())
                if case=='missing-source':raw['predicate']['buildDefinition']['resolvedDependencies']=[]
                else:raw['predicateType']='https://slsa.dev/provenance/v999'
                emit(root/'bad-native.json',raw);d['build']['provenance']=dict(path='bad-native.json',sha256=sha(root/'bad-native.json'))
            else:
                raw=json.loads((root/'scan.cdx.json').read_text())
                if case=='unsupported-bom':raw['specVersion']='999'
                elif case=='unknown-target':raw['vulnerabilities'][0]['affects'][0]['ref']='absent'
                else:raw['components'][0]['version']='2.0'
                emit(root/'bad-native.json',raw);d['scan']['result']=dict(path='bad-native.json',sha256=sha(root/'bad-native.json'))
            YAML_IO.dump(d,root/'changed.yaml');assert not run(root,'software','compile','--imports','software-imports.json','changed.yaml',code=1)
        original=(root/'host.zip').read_bytes();(root/'host.zip').write_bytes(original+b'x');run(root,'software','check','software.json',code=1);(root/'host.zip').write_bytes(original)
        (root/'provenance.intoto.json').unlink();run(root,'software','check','software.json',code=2)
    print('PASS native software: real build/SLSA/CycloneDX, deployed advisory-control chain, unknown/failed/clean scan, stale reviews, tampered/missing resources, version rejection, clean rebuild and YAML-free consumer')
if __name__=='__main__':verify()
