"""Experimental deterministic report over published verification output, standard library only."""
import argparse
import html
import json
import os
import sys
sys.dont_write_bytecode=True
from pathlib import Path
from urllib.parse import quote, urlsplit


def unique(pairs):
    result={}
    for key,value in pairs:
        if key in result:raise ValueError('duplicate JSON key')
        result[key]=value
    return result


def render(a,bases):
    attribute_mode=a['format']=='mundane-verification-0.2'
    if a['format'] not in ('mundane-verification-0.1','mundane-verification-0.2') or a['complete'] is not True or a['diagnostics']:
        raise ValueError('analysis is unsupported or incomplete')
    linked=a['linked']
    if linked['format']!=('mundane-linked-0.2' if attribute_mode else 'mundane-linked-0.1') or linked['complete'] is not True or linked['diagnostics']:
        raise ValueError('linked inputs are unsupported or incomplete')
    plan=linked['planArtifact']['artifact']
    if plan['format']!='mundane-plan-0.1' or plan['complete'] is not True or plan['diagnostics']:
        raise ValueError('plan is unsupported or incomplete')
    for base in bases.values():
        if urlsplit(base).scheme not in ('','http','https'):raise ValueError('unsupported source-link scheme')
    imports={i['scope']:i for i in linked['imports']}
    if len(imports)!=len(linked['imports']):raise ValueError('duplicate import scope')
    records={}
    for scope,imported in imports.items():
        artifact=imported['artifact']
        if artifact['format'] not in (('mundanereq-requirements-0.1','mundanereq-requirements-0.2') if attribute_mode else ('mundanereq-requirements-0.1',)) or artifact['complete'] is not True or artifact['diagnostics']:
            raise ValueError('requirement import unsupported or incomplete')
        for record in artifact['requirements']:
            key=(scope,record['values']['id'])
            if key in records:raise ValueError('duplicate requirement ID')
            records[key]=record
    edges={(e['planId'],e['activityId'],e['requirementId']):e for e in linked['edges']}
    rows={(r['planId'],r['activityId'],r['requirementId']):r for r in a['coverage']}
    if len(edges)!=len(linked['edges']) or len(rows)!=len(a['coverage']) or rows.keys()!=edges.keys():raise ValueError('analysis rows do not match linked assertions')
    for key,row in rows.items():
        if any(row[k]!=v for k,v in edges[key].items()):raise ValueError('analysis row changed authored reference')
        if row['state'] not in ('current','review-stale') or bool(row['changedFields'])!=(row['state']=='review-stale') or row['possibleImpact']!=bool(row['changedFields']):raise ValueError('invalid review state')
        if (row['currentScope'],row['requirementId']) not in records:raise ValueError('missing displayed requirement')
    if attribute_mode:
        from attribute_input import validate
        schemas=validate(a)
    def esc(value):return html.escape(str(value),quote=True)
    def anchor(scope,id):return 'requirement/'+scope+':'+id
    def source_link(scope,path,line):
        if not isinstance(line,int) or isinstance(line,bool) or line<1:raise ValueError('invalid source line')
        if path.startswith('/') or '\\' in path or any(p in ('','..','.') for p in path.split('/')):raise ValueError('invalid source path')
        label=esc(scope+':'+path+':'+str(line))
        return '<a href="'+esc(bases[scope].rstrip('/')+'/'+quote(path,safe='/')+'#L'+str(line))+'">'+label+'</a>' if scope in bases else label
    def location(loc):
        scope,path=loc['path'].split(':',1);return source_link(scope,path,loc['line'])
    def schema_link(scope):
        schema=imports[scope]['artifact'].get('attributeSchema')
        if schema is None:return '(no schema selected)'
        return source_link(scope,schema['source']['path'],1)+'; SHA-256 '+esc(schema['source']['sha256'])
    def attribute_changes(row):
        if not attribute_mode or not (row['changedAttributes'] or row['schemaChanged']):return ''
        out=['<details><summary>Attribute and declaration changes</summary>']
        for label,key in [('Baseline','baselineScope'),('Current','currentScope')]:
            scope=row[key];v=records[(scope,row['requirementId'])]['values'];loc=records[(scope,row['requirementId'])]['locations']['record']
            out.append('<h4>'+label+'</h4><p>'+source_link(scope,loc['path'],loc['start']['line'])+'</p><p>'+schema_link(scope)+'</p><pre>'+esc(json.dumps({'attributes':v.get('attributes',{}),'definition':schemas[scope]},ensure_ascii=False,sort_keys=True,indent=2))+'</pre>')
        return ''.join(out)+'</details>'
    def attribute_table(scope,record):
        schema=imports[scope]['artifact'].get('attributeSchema')
        if schema is None:return '<h4>Project attributes</h4><p>No schema selected; no project attributes.</p>'
        out=['<h4>Project attributes</h4><p>'+schema_link(scope)+'</p><table><tr><th>Name / type</th><th>Present value</th><th>Declaration</th><th>Source</th></tr>']
        for name,d in sorted(schemas[scope]['attributes'].items()):
            value=record['values']['attributes'].get(name);decl=schema['locations'][name]
            links=source_link(scope,decl['path'],decl['start']['line'])
            if value is not None:
                loc=record['locations']['attributes'][name]['value'];links+='; value '+source_link(scope,loc['path'],loc['start']['line'])
            out.append('<tr><td>'+esc(name)+' / '+esc(d['type'])+'</td><td>'+esc(value if value is not None else '(not supplied)')+'</td><td>'+esc(d['description'])+'; '+('required' if d['required'] else 'optional')+'</td><td>'+links+'</td></tr>')
        return ''.join(out)+'</table>'
    def blocks(values):
        result=[]
        for block in values or []:
            if block['kind']=='prose':result.append('<p>'+esc(block['text'])+'</p>')
            elif block['kind']=='math' and block['language']=='latex':result.append('<pre aria-label="Opaque LaTeX">'+esc(block['payload'])+'</pre>')
            else:raise ValueError('unsupported statement block')
        return ''.join(result)
    stale=sum(r['state']=='review-stale' for r in rows.values())
    out=['<!doctype html>','<html lang="en"><meta charset="utf-8"><title>Verification review report</title>',
         '<style>body{font:16px/1.5 system-ui,sans-serif;margin:2rem auto;max-width:1100px;padding:0 1rem;color:#182536}h1,h2{color:#183d62}table{border-collapse:collapse;width:100%;margin:1rem 0}th,td{text-align:left;border-bottom:1px solid #ccd5df;padding:.45rem;vertical-align:top}a{color:#175c98}pre{white-space:pre-wrap;background:#f1f4f7;padding:1rem}code{overflow-wrap:anywhere}.stale{color:#8b3e06;font-weight:bold}section{border-top:1px solid #ccd5df;margin-top:2rem}.notice{background:#eef4fa;padding:1rem}details{margin:1rem 0}</style>',
         '<h1>Verification review report</h1>',
         '<p class="notice">Generated from requirement and verification-plan source. Edit source and regenerate this report. Planned coverage and review state do not establish execution, evidence adequacy, or requirement satisfaction.</p>',
         '<p>Analysis: complete. Context: '+esc(a['context'] or 'all declared plans')+'. Plans: '+esc(', '.join(linked['plans']))+'.</p>',
         f'<p>Planned assertions: {len(rows)}. Review-stale: {stale}. Uncovered requirements by plan: {len(a["uncovered"])}.</p>',
         '<p>Regenerate: run mundane-plan and mundane-verify with recorded imports, then render.py ANALYSIS.json with the same source-link bases. Check command exit status before using output.</p>',
         '<h2>Planned coverage and review findings</h2><table><tr><th>Plan / context</th><th>Activity</th><th>Requirement</th><th>Review basis</th><th>Authored assertion</th></tr>']
    review=[]
    for key,row in sorted(rows.items()):
        if row['state']=='review-stale':
            href='#'+quote(anchor(row['currentScope'],row['requirementId']),safe='')
            review.append('<li><a href="'+href+'">'+esc(row['requirementId'])+'</a> — '+esc(row['activityId'])+'; changed '+esc(', '.join(row['changedFields']))+'; '+location(row['location'])+attribute_changes(row)+'</li>')
    out.insert(-1,'<h2>Needs review</h2>'+('<ul>'+''.join(review)+'</ul>' if review else '<p>No stale requirement bindings.</p>'))
    activities={v['id']:v for v in plan['activities']}
    for key,row in sorted(rows.items()):
        activity=activities[row['activityId']];href='#'+quote(anchor(row['currentScope'],row['requirementId']),safe='')
        state=esc(row['state'])+(' — changed '+esc(', '.join(row['changedFields'])) if row['changedFields'] else '')
        out.append('<tr><td>'+esc(row['planId']+' / '+row['context'])+'</td><td>'+esc(row['activityId'])+'<br>'+esc(activity['method'])+'</td><td><a href="'+href+'">'+esc(row['currentScope']+':'+row['requirementId'])+'</a></td><td class="'+('stale' if row['changedFields'] else 'current')+'">'+state+'</td><td>'+location(row['location'])+'</td></tr>')
    out+=['</table>','<h2>Uncovered requirements</h2>']
    if not a['uncovered']:out.append('<p>None in the selected plans.</p>')
    for row in a['uncovered']:
        if (row['scope'],row['requirementId']) not in records:raise ValueError('missing uncovered requirement')
        out.append('<p>'+esc(row['planId']+' / '+row['context']+' / '+row['requirementId'])+' — '+location(row['location'])+'</p>')
    out.append('<h2>Activity definitions</h2>')
    for id,activity in sorted(activities.items()):
        out.append('<details><summary>'+esc(id+' — '+activity['method'])+'</summary><p>'+esc(activity['objective'])+'</p><p>Expected evidence: '+esc(activity['expectedEvidence'])+'</p><p>'+source_link('plan',activity['location']['path'],activity['location']['line'])+'</p></details>')
    out.append('<h2>Current requirement values</h2>')
    scopes={p['currentScope'] or next(iter(imports)) for p in plan['plans'] if p['id'] in linked['plans']}
    for (scope,id),record in sorted(records.items()):
        if scope not in scopes:continue
        v=record['values'];loc=record['locations']['record']
        out+=['<section id="'+esc(anchor(scope,id))+'"><h3>'+esc(scope+':'+id+' — '+v['title'])+'</h3>',
              '<p>'+source_link(scope,loc['path'],loc['start']['line'])+'</p>',
              '<p>Allocation: '+esc(v['allocation'] if v['allocation'] is not None else '(absent)')+'</p>',blocks(v['statement']),
              '<h4>Rationale</h4>'+blocks(v['rationale']),'<p>Authored source citation: '+esc(v['source'] if v['source'] is not None else '(absent)')+'</p>',
              '<p>Decomposes: '+', '.join('<a href="#'+quote(anchor(scope,target),safe='')+'">'+esc(target)+'</a>' for target in v['decomposes'])+'</p>'+ (attribute_table(scope,record) if attribute_mode else '')+'</section>']
    if attribute_mode:
        out=[line.replace('Generated from requirement and verification-plan source.','Generated from requirement, attribute-declaration and verification-plan source.') for line in out]
    metadata={'analysisFormat':a['format'],'analyzer':a['analyzer'],'linker':linked['linker'],'linkFormat':linked['format'],
              'plan':{k:v for k,v in linked['planArtifact'].items() if k!='artifact'},'planCompiler':plan['compiler'],'planFormat':plan['format'],'planSources':plan['sources'],
              'imports':[{k:v for k,v in imported.items() if k!='artifact'}|{'compiler':imported['artifact']['compiler'],'format':imported['artifact']['format'],'sourceContract':imported['artifact']['sourceContract'],'sources':imported['artifact']['sources']} for _,imported in sorted(imports.items())],
              'sourceLinkBases':bases}
    out+=['<h2>Selected inputs and provenance</h2><pre>'+esc(json.dumps(metadata,sort_keys=True,ensure_ascii=False,indent=2))+'</pre>','</html>']
    return ('\n'.join(out)+'\n').encode('utf-8')


def main():
    parser=argparse.ArgumentParser();parser.add_argument('analysis');parser.add_argument('--source-base',action='append',default=[])
    args=parser.parse_args()
    try:
        bases={}
        for item in args.source_base:
            scope,base=item.split('=',1)
            if scope in bases or not base:raise ValueError('duplicate or empty source-link base')
            bases[scope]=base
        with Path(args.analysis).open('rb') as stream:data=stream.read(32*1024*1024+1)
        if len(data)>32*1024*1024:raise ValueError('analysis exceeds 32 MiB')
        a=json.loads(data.decode('utf-8'),object_pairs_hook=unique)
        rendered=render(a,bases)
        os.write(2,b'')  # A closed diagnostic stream must not report successful completion.
        remaining=memoryview(rendered)
        while remaining:
            written=os.write(1,remaining)
            if written==0:raise OSError("output made no progress")
            remaining=remaining[written:]
        return 0
    except (OSError,ValueError,KeyError,TypeError,UnicodeError,RecursionError) as error:
        try:os.write(2,('report-failed: '+str(error)+'\n').encode('utf-8',errors='replace'))
        except OSError:pass
        # Avoid a second buffered broken-pipe exception at interpreter shutdown.
        try:
            with open(os.devnull,'wb') as sink:os.dup2(sink.fileno(),1)
        except OSError:pass
        return 2


if __name__=='__main__':sys.exit(main())
