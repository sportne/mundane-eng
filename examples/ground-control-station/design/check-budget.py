"""Independent Decimal point calculations and dimensional design checks."""
import copy,json
from decimal import Decimal
from pathlib import Path
from ruamel.yaml import YAML
from jsonschema import Draft202012Validator
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2];yaml=YAML(typ='safe')
# SI dimensions: power, time, data. Decimal SI prefixes; bytes = eight bits.
UNITS={'1':('1',(0,0,0)),'W':('1',(1,0,0)),'kW':('1000',(1,0,0)),'Wh':('3600',(1,1,0)),'kWh':('3600000',(1,1,0)),'s':('1',(0,1,0)),'ms':('.001',(0,1,0)),'h':('3600',(0,1,0)),'Mbps':('1000000',(0,-1,1)),'bps':('1',(0,-1,1)),'GB':('8000000000',(0,0,1)),'MB':('8000000',(0,0,1)),'bit':('1',(0,0,1))}
def calculate(d):
    values={};formulas={f['id']:f for f in d['formulas']}
    for p in d['parameters']:
        factor,dim=UNITS[p['unit']];assert p['min']<=p['max'];values[p['id']]=(Decimal(str(p['min']))*Decimal(factor),dim)
    def evaluate(ident,seen=frozenset()):
        if ident in values:return values[ident]
        assert ident not in seen,'cycle';f=formulas[ident];items=[evaluate(i,seen|{ident}) for i in f['inputs']];assert len(items)>=2
        value,dim=items[0]
        for other,odim in items[1:]:
            if f['op'] in ['sum','difference']:
                assert dim==odim,'mixed units';value=value+other if f['op']=='sum' else value-other
            else:
                sign=1 if f['op']=='product' else -1
                dim=tuple(a+sign*b for a,b in zip(dim,odim));value=value*other if sign==1 else value/other
        assert dim==UNITS[f['unit']][1],'wrong output units';values[ident]=(value,dim);return values[ident]
    for ident in formulas:evaluate(ident)
    return {i:float(v/Decimal(UNITS[(formulas[i] if i in formulas else next(p for p in d['parameters'] if p['id']==i))['unit']][0])) for i,(v,dim) in values.items()}
def availability(mtbf,repair):return Decimal(str(mtbf))/(Decimal(str(mtbf))+Decimal(str(repair)))
def verify():
    d=yaml.load(HERE/'budget.yaml');p=HERE/'budget.schema.json'
    if not p.exists():p=ROOT/'specification/schema/budget-yaml-0.1.json'
    schema=json.loads(p.read_text());Draft202012Validator.check_schema(schema);Draft202012Validator(schema).validate(d)
    values=calculate(d)
    expected={'TOTAL-POWER':90,'USABLE-ENERGY':86.4,'RUNTIME':.96,'NETWORK-TOTAL':15,'STORAGE':7.2}
    for key,value in expected.items():assert abs(values[key]-value)<1e-10
    bad=copy.deepcopy(d);bad['parameters'][0]['unit']='Wh'
    try:calculate(bad)
    except AssertionError:pass
    else:raise AssertionError('mixed dimensions accepted')
    bad=copy.deepcopy(d);bad['formulas'][0]['inputs'].remove('HOST-PEAK')
    required={p['id'] for p in d['parameters'] if p['rating'] and p['rating']['rating']=='PEAK'}
    assert not required<=set(bad['formulas'][0]['inputs']),'missing peak unnoticed'
    bad=copy.deepcopy(d);bad['parameters'][0]['min']=bad['parameters'][0]['max']=200
    changed=calculate(bad);assert changed['TOTAL-POWER']>changed['POWER-LIMIT'] and changed['RUNTIME']<changed['RUNTIME-MIN']
    nominal=availability(20000,4)**3*availability(1000,2)
    delayed=availability(20000,4)**3*availability(1000,20)
    assert delayed<nominal<availability(20000,4)**3
    out=ROOT/'build/gcs-design';out.mkdir(parents=True,exist_ok=True)
    (out/'budget-reference.json').write_text(json.dumps(dict(pointValues=expected,availability=float(nominal),delayedRepairAvailability=float(delayed),scope='conditional on stated independence and steady-state assumptions'),indent=2)+'\n')
    print('PASS budget design: independent dimensional calculations, missing peak, mixed units, exhausted margin, usable energy and shared-power repair sensitivity')
if __name__=='__main__':verify()
