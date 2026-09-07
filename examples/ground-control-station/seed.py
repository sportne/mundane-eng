"""GCS engineering seed: existing public tools, explicit pins and negative cases."""
import hashlib
import json
import re
from urllib.parse import unquote
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).parent/'seed'
OUT = ROOT/'build/gcs-seed'
BIN = ROOT/'build/maintained'

def write(path, value):
    path.write_text(json.dumps(value,sort_keys=True,ensure_ascii=False,separators=(',',':'))+'\n')

def run(root, tool, args, expected=0):
    p = subprocess.run([str(BIN/tool),*args],cwd=root,capture_output=True,timeout=45)
    if p.returncode != expected:
        raise AssertionError((tool,args,expected,p.returncode,p.stderr.decode(),p.stdout[-1600:].decode()))
    return p.stdout

def requirements(root, path='source'):
    return run(root,'mundanereq-compile',['--source=yaml-0.4','--root','.',
        '--attribute-schema',path+'/attributes.yaml',path+'/requirements.yaml'])

def imports(root):
    entries=[{'scope':scope,'path':scope+'.json','kind':kind,'sha256':hashlib.sha256((root/(scope+'.json')).read_bytes()).hexdigest(),'dependsOn':[]}
        for scope,kind in [('baseline','requirements'),('current','requirements'),('plan','verification-plan'),('work','work-items')]]
    write(root/'imports.json',{'format':'mundane-imports-0.1','imports':entries})
    write(root/'work-imports.json',{'format':'mundane-imports-0.1','imports':entries[:3]})
    write(root/'verification-imports.json',{'format':'mundane-imports-0.1','imports':entries[:2]})

def analysis(root):
    args=['--root','.','--plan','plan.json','verification-imports.json']
    (root/'linked.json').write_bytes(run(root,'mundane-link',args))
    raw=run(root,'mundane-verify',args,1)  # Deliberately uncovered assurance requirement.
    (root/'analysis.json').write_bytes(raw)
    return json.loads(raw)

def generate(root):
    root.mkdir(parents=True,exist_ok=True)
    shutil.copytree(SOURCE,root/'source',dirs_exist_ok=True)
    shutil.copytree(SOURCE,root/'baseline-source',dirs_exist_ok=True)
    (root/'baseline.json').write_bytes(requirements(root,'baseline-source'))
    (root/'current.json').write_bytes(requirements(root))
    (root/'plan.json').write_bytes(run(root,'mundane-plan',['--root','.','source/plan']))
    (root/'work.json').write_bytes(run(root,'mundane-work',['compile','--root','.','source/work-items.json']))
    imports(root)
    a=analysis(root)
    assert a['complete']
    (root/'work-analysis.json').write_bytes(run(root,'mundane-work',['analyze','--root','.','--imports','work-imports.json','work.json']))
    (root/'WORK.md').write_bytes(run(root,'mundane-work',['view','--root','.','work-analysis.json']))
    (root/'impact.json').write_bytes(run(root,'mundane-impact',['query','--root','.','--from','current:requirement:GCS-STALE','imports.json']))
    (root/'IMPACT.md').write_bytes(run(root,'mundane-impact',['view','--root','.','impact.json']))
    p=subprocess.run([sys.executable,str(ROOT/'experiments/0029-verification-report/render.py'),str(root/'analysis.json'),
        '--source-base','baseline=.','--source-base','current=.','--source-base','plan=.'],capture_output=True,timeout=30)
    assert p.returncode==0,p.stderr
    (root/'coverage.html').write_bytes(p.stdout)
    for href in re.findall(r'href="([^"]+)"',p.stdout.decode()):
        path=unquote(href.split('#')[0])
        if path:
            assert (root/path).is_file(),('missing report source',href)
    uncovered=a['uncovered']
    assert any(x['requirementId']=='GCS-EVIDENCE' for x in uncovered), uncovered
    affected=[x['node'] for x in json.loads((root/'impact.json').read_bytes())['query']['affected']]
    assert 'work:work-item:GCS-TASK-STALE' in affected and 'work:work-item:GCS-TASK-REVIEW' in affected,affected
    # Source-linked report resources are local selected snapshots.
    assert (root/'source/requirements.yaml').is_file() and (root/'source/plan/plan.yaml').is_file()
    return a,affected

def verify():
    if OUT.exists():shutil.rmtree(OUT)
    a,affected=generate(OUT)
    expected={p.name:p.read_bytes() for p in OUT.iterdir() if p.is_file()}
    with tempfile.TemporaryDirectory(prefix='gcs-seed-rebuild-') as tmp:
        clean=Path(tmp);generate(clean)
        assert expected=={p.name:p.read_bytes() for p in clean.iterdir() if p.is_file()},'rebuild differs'
        # Change a covered requirement using real source compilation, retain baseline pin.
        p=clean/'source/requirements.yaml';p.write_text(p.read_text().replace('at least 500 ms','at least 400 ms'))
        (clean/'current.json').write_bytes(requirements(clean));imports(clean)
        changed=analysis(clean)
        stale=[row['requirementId'] for row in changed['coverage'] if row['state']=='review-stale']
        assert stale==['GCS-STALE'],stale
        # A missing required reference is a source diagnostic, not accepted coverage.
        p.write_text(p.read_text().replace('"id": "GCS-STALE"','"id": "GCS-STALE"\n    "decomposes": ["GCS-MISSING"]'))
        bad=run(clean,'mundanereq-compile',['--source=yaml-0.4','--root','.',
            '--attribute-schema','source/attributes.yaml','source/requirements.yaml'],1)
        assert b'dangling-reference' in bad,bad
        # A pin mismatch is independently rejected before semantic analysis.
        (clean/'current.json').write_bytes((clean/'current.json').read_bytes()+b' ')
        bad=run(clean,'mundane-link',['--root','.','--plan','plan.json','verification-imports.json'],1)
        assert b'digest-mismatch' in bad,bad
    write(OUT/'evidence.json',{'scope':'engineering-tool demonstration, not simulated vehicle execution',
        'requirements':10,'plannedCoverage':len(a['coverage']),'deliberatelyUncovered':['GCS-EVIDENCE'],
        'impactCandidates':affected,'checks':['identical-clean-rebuild','changed-requirement-review-stale',
        'missing-reference-rejected','changed-pinned-resource-rejected']})
    print('PASS GCS seed: ten requirements, planned coverage/uncovered distinction, linked work, explained impact, deterministic clean rebuild, stale review and rejected missing reference/pin')
    print(OUT/'coverage.html')

if __name__=='__main__':
    verify()
