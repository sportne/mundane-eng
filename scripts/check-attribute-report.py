"""Attribute-aware derived display."""
import copy,json,subprocess,tempfile
from pathlib import Path
from html.parser import HTMLParser
ROOT=Path(__file__).resolve().parents[1];HERE=ROOT/'experiments/0036-project-attributes';RENDER=ROOT/'experiments/0029-verification-report/render.py';BIN=ROOT/'build/maintained';A=json.loads((HERE/'golden/verification.json').read_text())
class Parsed(HTMLParser):
 def __init__(self):super().__init__();self.tags=[];self.text=[];self.links=[]
 def handle_starttag(self,t,a):
  self.tags.append(t)
  if t=='a':self.links.append(dict(a)['href'])
 def handle_data(self,d):self.text.append(d)
with tempfile.TemporaryDirectory(prefix='attribute-report-') as tmp:
 root=Path(tmp);p=root/'analysis.json'
 def render(a,status=0,extra=[]):
  p.write_text(json.dumps(a,ensure_ascii=False));r=subprocess.run(['python3',str(RENDER),str(p),'--source-base','current=https://example.invalid/current','--source-base','baseline=https://example.invalid/baseline','--source-base','plan=https://example.invalid/plan']+extra,capture_output=True,timeout=30);assert r.returncode==status,(r.stderr,r.stdout[:200]);assert (not r.stdout and r.stderr) if status else not r.stderr;return r.stdout
 report=render(A);p.unlink();assert render(A)==report
 assert report==(HERE/'golden/report.html').read_bytes()
 parsed=Parsed();parsed.feed(report.decode());text=' '.join(parsed.text)
 assert 'Controls 😀 <review>' in text and 'Revised <team> description' in text and '(not supplied)' in text and 'required' in text and 'optional' in text and 'attribute-declaration' in text
 assert 'review' not in parsed.tags and 'team' not in parsed.tags and b'&lt;review&gt;' in report
 assert any('/schema.json#L' in href for href in parsed.links) and any('/source.mreq.yaml#L' in href for href in parsed.links) and any('/coverage.tsv#L' in href for href in parsed.links)
 for change in [lambda a:a.update(complete=False),lambda a:a.update(format='future'),lambda a:a['linked'].update(format='mundane-linked-0.1'),lambda a:a['coverage'][0].update(changedAttributes=[]),lambda a:a['coverage'][0].update(schemaChanged=False),lambda a:a['coverage'][0].update(possibleImpact=1),lambda a:a['coverage'][0].update(changedFields=[]),lambda a:a['linked']['imports'][1]['artifact']['requirements'][0]['values']['attributes'].update(discipline='bad'),lambda a:a['linked']['imports'][1]['artifact']['attributeSchema']['definition'].update(format='future'),lambda a:a['linked']['imports'][1]['artifact']['requirements'][0]['locations']['attributes'].pop('discipline'),lambda a:a['linked']['imports'][1]['artifact']['attributeSchema']['source'].update(path='../bad'),lambda a:a['linked']['imports'][1]['artifact']['attributeSchema']['locations']['discipline']['end'].update(line=0)]:
  bad=copy.deepcopy(A);change(bad);render(bad,2)
 render(A,2,['--source-base','other=javascript:alert(1)'])
 large=copy.deepcopy(A);large['linked']['imports'][1]['artifact']['requirements'][0]['values']['attributes']['owner-team']='x'*500000;render(large)
 args=['python3',str(RENDER),str(p)]
 process=subprocess.Popen(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE);assert process.stdout.read(1);process.stdout.close();assert process.wait(timeout=30)==2;process.stderr.close()
 for fd in [1,2]:assert subprocess.run(['bash','-c',f'exec "$@" {fd}>&-','closed']+args,capture_output=True,timeout=30).returncode==2
print('PASS attribute report: golden, typed values, escaped text, provenance, tamper rejection and output failures')
