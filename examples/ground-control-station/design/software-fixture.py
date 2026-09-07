"""Offline fixture producer: actual deterministic archive build, unsigned native records."""
import hashlib
import json
from pathlib import Path
import zipfile

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def emit(path, value): path.write_text(json.dumps(value, sort_keys=True, indent=2)+"\n")
def build(root):
    root.mkdir(parents=True,exist_ok=True)
    source=root/'host-source.py'
    source.write_text('"""Synthetic offline host; no aircraft or network access."""\ndef state(age_ms):\n    return "stale" if age_ms > 500 else "fresh"\n')
    dependency=root/'link-fixture.py'
    dependency.write_text('"""Illustrative dependency, not a third-party product."""\nVERSION = "1.0"\n')
    recipe=root/'build-recipe.txt'
    recipe.write_text('fixture-zip-v1: ZIP_STORED, 1980-01-01 UTC entries, sorted source names; no environment inputs\n')
    binary=root/'host.zip'
    with zipfile.ZipFile(binary,'w',compression=zipfile.ZIP_STORED) as z:
        for p in sorted([source,dependency]):
            info=zipfile.ZipInfo(p.name,(1980,1,1,0,0,0));info.external_attr=0o100644<<16
            z.writestr(info,p.read_bytes())
    def resource(p):return dict(path=p.name,sha256=sha(p))
    statement={'_type':'https://in-toto.io/Statement/v1','subject':[dict(name='host.zip',digest=dict(sha256=sha(binary)))],
      'predicateType':'https://slsa.dev/provenance/v1','predicate':{'buildDefinition':{'buildType':'urn:mundane-eng:fixture:zip-v1','externalParameters':{'sourceUri':'urn:mundane-eng:fixture:host-source','recipeUri':'urn:mundane-eng:fixture:build-recipe'},'resolvedDependencies':[dict(uri=uri,digest=dict(sha256=sha(p))) for uri,p in [('urn:mundane-eng:fixture:host-source',source),('urn:mundane-eng:fixture:build-recipe',recipe),('urn:mundane-eng:fixture:link',dependency)]]},'runDetails':{'builder':{'id':'urn:mundane-eng:fixture:offline-builder','version':{'fixture':'1'}}}}}
    emit(root/'provenance.intoto.json',statement)
    component={'type':'application','bom-ref':'gcs-host','name':'synthetic-gcs-host','version':'1','hashes':[{'alg':'SHA-256','content':sha(binary)}]}
    lib={'type':'library','bom-ref':'link-fixture','name':'synthetic-link','version':'1.0','hashes':[{'alg':'SHA-256','content':sha(dependency)}]}
    bom={'bomFormat':'CycloneDX','specVersion':'1.6','version':1,'metadata':{'component':component},'components':[lib],'dependencies':[{'ref':'gcs-host','dependsOn':['link-fixture']},{'ref':'link-fixture','dependsOn':[]}]}
    emit(root/'bom.cdx.json',bom)
    vex=dict(bom,vulnerabilities=[{'id':'EXAMPLE-REPLAY-001','source':{'name':'synthetic-offline-fixture'},'description':'Illustrative replay exposure, not a real advisory','affects':[{'ref':'link-fixture'}],'analysis':{'state':'in_triage','detail':'Synthetic observation; exploitability not established.'}}])
    emit(root/'scan.cdx.json',vex)
    return dict(binary=resource(binary),source=dict(resource(source),uri='urn:mundane-eng:fixture:host-source'),recipe=dict(resource(recipe),uri='urn:mundane-eng:fixture:build-recipe'),provenance=resource(root/'provenance.intoto.json'),sbom=resource(root/'bom.cdx.json'))
