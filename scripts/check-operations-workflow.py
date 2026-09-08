"""Native GCS commissioning/recovery history and corrective-work trace, without deployment."""
import copy,importlib.util,json,shutil,subprocess,tempfile
from datetime import datetime,timedelta,timezone
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('assurance_workflow',ROOT/'scripts/check-assurance-workflow.py');a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)
yaml=a.yaml;run=a.run;sha=a.sha;emit=a.emit;selection=a.selection;HERE=a.HERE;AT=a.AT

def compile_ops(root,source,name='operations',code=0):
    yaml.dump(source,root/(name+'.yaml'));data=run(root,'operations','compile','--imports','operations-imports.json',name+'.yaml',code=code)
    if code==0:(root/(name+'.json')).write_bytes(data)
    return data

def analyze(root,name='operations',at=AT):return json.loads(run(root,'operations','analyze',name+'.json',at,'trust.jwks.json'))
def candidate(result,id):return next(c for c in result['candidates'] if c['id']==id)

def work(root,status='Complete',name='corrective-work'):
    d=dict(format='mundane-work-yaml-0.2',id='GCS-CORRECT-QUEUE',kind='task',title='Review queued-command recovery after the synthetic anomaly',status=status,dependencies=[],relations=[dict(relation='addresses',scope='gcs-req',kind='requirement',target='GCS-RESTART')],body='Synthetic corrective review and replacement simulation completed. This is no field repair or certification claim.')
    yaml.dump(d,root/(name+'.yaml'));emit(root/(name+'-set.json'),dict(format='mundane-work-set-0.2',source='mundane-work-yaml-0.2',files=[name+'.yaml']))
    (root/(name+'.json')).write_bytes(run(root,'work','compile',str(root/(name+'-set.json'))))
    return dict(path=name+'.json',sha256=sha(root/(name+'.json')),id=d['id'])

def stage(root):
    shutil.copytree(ROOT/'build/gcs-assurance',root,dirs_exist_ok=True)
    # A second, explicitly revised procedure produces a distinct actual simulator run.
    p=yaml.load(root/'procedure-combined.yaml');p['id']='PROC-CORRECTED';p['objective']='Repeat queue inhibition after synthetic corrective review under a new session.'
    p['session']='SESSION-CORRECTED'
    for event in p['events']:
        if 'session' in event:event['session']='SESSION-CORRECTED' if event['kind']!='restart' else 'SESSION-AFTER-CORRECTION'
        if 'request' in event:event['request']='REQUEST-CORRECTED'
    yaml.dump(p,root/'procedure-corrected.yaml');(root/'procedure-corrected.json').write_bytes(run(root,'procedure','compile','--imports','procedure-imports.json','procedure-corrected.yaml'))
    (root/'run-corrected.json').write_bytes(run(root,'evidence','simulate','procedure-corrected.json','runtime/mundane-evidence','nominal'))
    (root/'evidence-corrected.json').write_bytes(run(root,'evidence','import','procedure-corrected.json','run-corrected.json'))
    entries=json.loads((root/'assurance-imports.json').read_text())['imports']
    entries.extend([selection(root,'gcs-assurance','assurance','waived.json'),selection(root,'proc-corrected','procedure','procedure-corrected.json'),selection(root,'evidence-corrected','evidence','evidence-corrected.json')])
    emit(root/'operations-imports.json',dict(format='mundane-domain-imports-0.1',imports=entries))
    source=yaml.load(HERE/'design/operations.yaml');initial=source['candidates'][0];initial['buildSha256']=sha(root/'host.zip')
    previous='RC-INITIAL'
    for id,plan in [('RC-UPGRADE','PLAN-UPGRADE'),('RC-REPLACEMENT','PLAN-REPLACEMENT'),('RC-CORRECTED','PLAN-CORRECTED')]:
        c=copy.deepcopy(initial);c.update(id=id,predecessor=previous,commissioningPlans=[plan]);source['candidates'].append(c);previous=id
    plan=copy.deepcopy(source['plans'][0]);plan.update(id='PLAN-CORRECTED',procedure=dict(scope='proc-corrected',kind='procedure',id='PROC-CORRECTED'));source['plans'].append(plan)
    (root/'backup.native').write_bytes(b'Synthetic application-owned backup fixture; contents do not establish restore correctness.\n')
    backup=dict(path='backup.native',sha256=sha(root/'backup.native'))
    config=sha(root/'configuration-software.json');build=sha(root/'host.zip');start=datetime(2026,9,8,8,0,tzinfo=timezone.utc)
    previous=None
    sequence=[('STARTUP','RC-INITIAL','succeeded'),('BACKUP','RC-INITIAL','succeeded'),('UPGRADE','RC-UPGRADE','failed'),('ROLLBACK','RC-UPGRADE','succeeded'),('RESTORE','RC-INITIAL','succeeded'),('RESTART','RC-INITIAL','succeeded'),('HANDOVER','RC-INITIAL','succeeded'),('REPLACEMENT','RC-REPLACEMENT','succeeded'),('MAINTENANCE','RC-REPLACEMENT','failed'),('CORRECTED','RC-CORRECTED','succeeded')]
    for i,(kind,candidateId,outcome) in enumerate(sequence):
        begin=start+timedelta(minutes=i*5);end=begin+timedelta(minutes=1)
        iso=lambda t:t.isoformat().replace('+00:00','Z')
        e=dict(id='EXEC-'+kind,previous=previous,plan='PLAN-'+kind,candidate=candidateId,startedAt=iso(begin),finishedAt=iso(end),observedConfigurationSha256=config,observedBuildSha256=build,preconditions=[dict(id='INHIBITED',result='met')],evidenceScopes=['evidence-corrected' if kind=='CORRECTED' else 'evidence-combined'],actions=[dict(at=iso(begin),actor='fixture-operator',action='Recorded synthetic '+kind.lower()+' with command outputs inhibited; no field action.')],outcome=outcome,decision=dict(at=iso(end),actor='fixture-operator',role='gcs-operator',disposition='accepted' if outcome=='succeeded' else 'rejected',reason='Synthetic tabletop disposition; combined-fault simulator establishes only its bounded queue criteria.'),rollbackTo='RC-INITIAL' if kind=='ROLLBACK' else None,backup=copy.deepcopy(backup) if kind in ['BACKUP','RESTORE'] else None)
        source['executions'].append(e);previous=e['id']
    source['compatibility']=[dict(id='COMPAT-ROLLBACK',fromCandidate='RC-UPGRADE',toCandidate='RC-INITIAL',disposition='compatible',reason='Same exact synthetic build/baseline; retained combined-fault evidence. No real firmware/data migration compatibility claim.',evidenceScopes=['evidence-combined'])]
    source['incidents']=[dict(id='INC-QUEUE',candidate='RC-REPLACEMENT',execution='EXEC-MAINTENANCE',requirement=dict(scope='gcs-req',kind='requirement',id='GCS-RESTART'),correctiveWork=work(root),status='closed',resolution=dict(candidate='RC-CORRECTED',execution='EXEC-CORRECTED',evidenceScopes=['evidence-corrected'],actor='fixture-operator',at='2026-09-08T09:00:00Z',reason='Synthetic corrective work and distinct repeated session evidence; old anomaly remains retained.'))]
    compiled=compile_ops(root,source);return source,compiled

def verify():
    out=ROOT/'build/gcs-operations'
    if out.exists():shutil.rmtree(out)
    source,compiled=stage(out);r=analyze(out)
    assert r['authorization']=='none' and len(r['executions'])==10 and r['incidents'][0]['state']=='closed',r
    assert candidate(r,'RC-INITIAL')['localReadiness'] and candidate(r,'RC-CORRECTED')['localReadiness'],r
    assert not candidate(r,'RC-UPGRADE')['localReadiness'] and not candidate(r,'RC-REPLACEMENT')['localReadiness']
    emit(out/'analysis.json',r);(out/'operations.md').write_bytes(run(out,'operations','view','operations.json',AT,'trust.jwks.json'))
    cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','operations'],cwd=ROOT,text=True).strip();cp=':'.join(p for p in cp.split(':') if '/domain-source/' not in p and '/yaml/' not in p and '/work-source/' not in p and not p.endswith('.jar'))
    assert run(out,'operations','check','operations.json',command=['java','-cp',cp,'engineering.operations.OperationsMain'])==compiled
    assert len(json.loads(run(out,'operations','query','operations.json',AT,'trust.jwks.json','RC-CORRECTED'))['candidates'])==1
    with tempfile.TemporaryDirectory(prefix='operations-rebuild-') as tmp:
        root=Path(tmp);shutil.copytree(out,root,dirs_exist_ok=True);assert compile_ops(root,source)==compiled
        for label in ['wrong-build','missing-evidence','drift','precondition','actions','acceptance','open-incident','reused-run','wrong-descendant','incomplete-work','unknown-rollback','missing-backup','cycle','unordered']:
            d=copy.deepcopy(source);expected=0;target='RC-CORRECTED';e=d['executions'][-1]
            if label=='wrong-build':d['candidates'][-1]['buildSha256']='0'*64
            elif label=='missing-evidence':e['evidenceScopes']=[]
            elif label=='drift':e['observedConfigurationSha256']='0'*64
            elif label=='precondition':e['preconditions'][0]['result']='unknown'
            elif label=='actions':e['actions']=[]
            elif label=='acceptance':e['decision']['disposition']='pending'
            elif label=='open-incident':d['incidents'][0]['status']='open'
            elif label=='reused-run':d['incidents'][0]['resolution']['evidenceScopes']=['evidence-combined']
            elif label=='wrong-descendant':d['candidates'][-1]['predecessor']='RC-INITIAL'
            elif label=='incomplete-work':d['incidents'][0]['correctiveWork']=work(root,'Planned','pending-work')
            elif label=='unknown-rollback':d['compatibility'][0]['disposition']='unknown';target='RC-UPGRADE'
            elif label=='missing-backup':d['executions'][4]['backup']=None;target='RC-INITIAL'
            elif label=='cycle':d['candidates'][0]['predecessor']='RC-CORRECTED';expected=1
            elif label=='unordered':d['executions'][1]['previous']=None;expected=1
            compile_ops(root,d,'changed',code=expected)
            if not expected:
                result=analyze(root,'changed');assert not candidate(result,target)['localReadiness'],label
                if label=='unknown-rollback':assert 'rollback-compatibility-unestablished' in result['executions'][3]['blockers']
        assert not candidate(analyze(root,at='2026-10-01T00:00:00Z'),'RC-CORRECTED')['localReadiness']
        assert not candidate(analyze(root,at='2026-09-08T07:00:00Z'),'RC-INITIAL')['localReadiness']
        # Default unwaived assurance blocks a candidate independently of commissioning success.
        imports=json.loads((root/'operations-imports.json').read_text());imports['imports']=[selection(root,'gcs-assurance','assurance','assurance.json') if i['scope']=='gcs-assurance' else i for i in imports['imports']];emit(root/'operations-imports.json',imports)
        compile_ops(root,source,'unwaived');assert 'assurance-unready' in candidate(analyze(root,'unwaived'),'RC-INITIAL')['blockers']
        (root/'operations.yaml').unlink();assert run(root,'operations','check','operations.json')==compiled
        (root/'backup.native').write_bytes(b'changed');run(root,'operations','check','operations.json',code=1)
        (root/'backup.native').unlink();run(root,'operations','check','operations.json',code=2)
    print('PASS native operations: retained failed upgrade, rollback/restore/restart, replacement, work/requirement/distinct-run/descendant closure; build/evidence/drift/review/history/operator blockers, clean rebuild and YAML-free consumers')
if __name__=='__main__':verify()
