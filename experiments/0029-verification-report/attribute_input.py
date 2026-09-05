"""Independent serialized validation for the experimental attribute report. No source loading."""
import copy
import re

FIELDS=('id','title','allocation','statement','rationale','source','decomposes')
RESERVED=set(FIELDS)|{'format','requirements','attributes','attribute-schema'}
def require(ok,message):
    if not ok:raise ValueError(message)
def obj(x):require(isinstance(x,dict),'expected object');return x
def keys(x,*names):require(set(obj(x))==set(names),'unexpected or missing schema/location fields')
def text(x):
    require(isinstance(x,str) and bool(x) and x==x.strip() and not any(ord(c)<32 or 127<=ord(c)<=159 or 0xd800<=ord(c)<=0xdfff for c in x),'invalid attribute text');return x

def name(x,attribute=False):
    require(isinstance(x,str) and len(x)<=64 and re.fullmatch('[a-z][a-z0-9]*(?:-[a-z0-9]+)*',x) is not None,'invalid declaration name')
    require(not attribute or x not in RESERVED and not x.startswith(('mreq-','mundane-')),'reserved attribute name')
def path(x):
    require(isinstance(x,str) and x and not x.startswith('/') and '\\' not in x and not any(ord(c)<32 for c in x) and all(p not in ('','.','..') for p in x.split('/')),'invalid source path');return x
def digest(x):require(isinstance(x,str) and re.fullmatch('[a-f0-9]{64}',x) is not None,'invalid digest')
def span(x,paths):
    obj(x);require(path(x['path']) in paths,'span outside source inventory')
    for p in ('start','end'):
        obj(x[p]);require(all(type(x[p][k]) is int and x[p][k]>0 for k in ('line','column')),'invalid source position')
    require((x['start']['line'],x['start']['column'])<=(x['end']['line'],x['end']['column']),'reversed span')
def definition(schema):
    if schema is None:return None
    keys(schema,'definition','source','locations');keys(schema['source'],'path','sha256');p=path(schema['source']['path']);digest(schema['source']['sha256'])
    d=copy.deepcopy(schema['definition']);keys(d,'format','name','attributes');require(d['format']=='mundanereq-attribute-schema-0.1','unsupported schema');name(d['name'])
    attrs=obj(d['attributes']);require(1<=len(attrs)<=128,'invalid declaration count');require(set(obj(schema['locations']))==set(attrs),'declaration location inventory mismatch')
    for n,a in attrs.items():
        name(n,True);obj(a);require(a['type'] in ('text','enum'),'unsupported type');keys(a,*(['type','required','description','values'] if a['type']=='enum' else ['type','required','description']))
        require(type(a['required']) is bool,'invalid requiredness');text(a['description']);span(schema['locations'][n],{p})
        if a['type']=='enum':
            require(isinstance(a['values'],list) and 1<=len(a['values'])<=256,'invalid enumeration')
            values=[text(v) for v in a['values']];require(len(set(values))==len(values),'duplicate enum');a['values']=sorted(values,key=lambda v:v.encode('utf-16-be'))
    return d

def artifact(a):
    require(a['artifactKind']=='requirements' and a['complete'] is True and a['diagnostics']==[],'incomplete requirement snapshot')
    new=a['format']=='mundanereq-requirements-0.2';require(new or a['format']=='mundanereq-requirements-0.1','unsupported requirements')
    require(a['sourceContract'] in (('mundanereq-yaml-0.4',) if new else ('mundanereq-source-0.2','mundanereq-yaml-0.3')),'source/output mismatch')
    require(new or 'attributeSchema' not in a,'schema in old output');d=definition(a['attributeSchema']) if new else None;decl={} if d is None else d['attributes']
    require(isinstance(a['sources'],list) and bool(a['sources']),'empty source inventory');paths=set()
    for s in a['sources']:
        p=path(s['path']);require(p not in paths,'duplicate source');paths.add(p);digest(s['sha256'])
    records={};require(isinstance(a['requirements'],list) and a['requirements'],'empty requirements')
    for r in a['requirements']:
        v=obj(r['values']);require(set(FIELDS)<=set(v),'missing builtin value');id=v['id'];require(isinstance(id,str) and re.fullmatch('[A-Za-z0-9][A-Za-z0-9._-]*',id) is not None and id not in records,'invalid/duplicate ID');records[id]=r
        require(isinstance(v['title'],str) and v['title'],'invalid title')
        for f in ('allocation','source'):require(v[f] is None or isinstance(v[f],str) and v[f],'invalid optional text')
        for f in ('statement','rationale'):
            if f=='rationale' and v[f] is None:continue
            require(isinstance(v[f],list) and v[f],'invalid body')
            for b in v[f]:
                obj(b)
                if b['kind']=='prose':require(isinstance(b['text'],str) and b['text'],'invalid prose')
                else:require(f=='statement' and b['kind']=='math' and b['language']=='latex' and isinstance(b['payload'],str) and b['payload'].strip('\n'),'invalid math')
        targets=v['decomposes'];require(isinstance(targets,list) and all(isinstance(t,str) for t in targets) and len(set(targets))==len(targets),'invalid references')
        loc=obj(r['locations']);span(loc['record'],paths);fields=obj(loc['fields']);require({'id','title','statement'}<=set(fields),'missing field spans')
        for f,points in fields.items():
            require(isinstance(points,list) and points,'empty field spans')
            for p in points:span(p,paths)
        for f in ('allocation','rationale','source'):
            require(v[f] is None or f in fields,'missing optional field span')
        require(not targets or 'decomposes' in fields,'missing decomposition span');require(set(obj(loc['references']))==set(targets),'reference location mismatch')
        for p in loc['references'].values():span(p,paths)
        if not new:require('attributes' not in v and 'attributes' not in loc,'attributes in old output');continue
        attrs=obj(v['attributes']);points=obj(loc['attributes']);require(set(attrs)<=set(decl) and set(points)==set(attrs),'unknown attribute or location mismatch')
        for n,a in decl.items():
            require(not a['required'] or n in attrs,'missing required attribute')
            if n in attrs:
                value=text(attrs[n]);require(a['type']!='enum' or value in a['values'],'invalid enum value');keys(points[n],'name','value');span(points[n]['name'],paths);span(points[n]['value'],paths)
    require(all(t in records for r in records.values() for t in r['values']['decomposes']),'unresolved decomposition')
    return d,records

def builtin(v):
    result={f:v[f] for f in FIELDS};result['decomposes']=sorted(v['decomposes'])
    for f in ('statement','rationale'):
        if v[f] is not None:result[f]=[{k:b[k] for k in (('kind','text') if b['kind']=='prose' else ('kind','language','payload'))} for b in v[f]]
    return result

def validate(a):
    linked=a['linked'];schemas={};records={}
    for i in linked['imports']:schemas[i['scope']],records[i['scope']]=artifact(i['artifact'])
    require(any(i['artifact']['format']=='mundanereq-requirements-0.2' for i in linked['imports']),'new analysis needs new requirement input')
    for row in a['coverage']:
        before=records[row['baselineScope']][row['requirementId']]['values'];after=records[row['currentScope']][row['requirementId']]['values'];b=builtin(before);c=builtin(after)
        changed=[f for f in FIELDS if b[f]!=c[f]];ba=before.get('attributes',{});ca=after.get('attributes',{})
        names=sorted(n for n in set(ba)|set(ca) if ba.get(n)!=ca.get(n));schema_changed=schemas[row['baselineScope']]!=schemas[row['currentScope']]
        if names:changed.append('attributes')
        if schema_changed:changed.append('attributeSchema')
        require(row['changedFields']==sorted(changed) and row['changedAttributes']==names and type(row['schemaChanged']) is bool and row['schemaChanged']==schema_changed,'findings disagree with imported meanings')
    return schemas
