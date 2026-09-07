'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { project, relative, invoke } = require('../src/client');
test('explicit project rejects traversal, ambiguity and unsupported profiles', () => {
  for (const p of ['../x', '/x', 'a//b', 'C:/file', 'a/./b']) assert.throws(() => relative(p));
  const valid = { format: 'mundane-editor-project-0.1', source: 'yaml-0.3', files: ['a.yaml'], attributeSchema: null };
  assert.equal(project(JSON.stringify(valid)).source, 'yaml-0.3');
  for (const changes of [{files:[]}, {files:['a.yaml','a.yaml']}, {source:'unknown'}, {attributeSchema:'schema.yaml'}, {extra:true}]) assert.throws(() => project(JSON.stringify({...valid,...changes})));
});
test('process errors and cancellation settle without hanging', async () => {
  await assert.rejects(invoke('relative', {}));
  await assert.rejects(invoke('/does/not/exist', {}));
  const abort = new AbortController(); abort.abort();
  await assert.rejects(invoke('/bin/cat', {}, abort.signal), /Cancelled/);
  await assert.rejects(invoke('/bin/cat', {}), /Unsupported/);
});
test('source code-point coordinates convert to UTF-16', () => {
  const { position } = require('../src/positions');
  assert.deepEqual(position('a😀b\r\nnext\n', 1, 3), { line: 0, character: 3 });
  assert.deepEqual(position('a😀b\r\nnext\n', 2, 2), { line: 1, character: 1 });
});
test('snapshot overlays buffers, preserves BOM and rejects invalid UTF-8 and outside paths', async () => {
  const fs = require('node:fs/promises'), os = require('node:os'), path = require('node:path');
  const { snapshot } = require('../src/client');
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'mundane-editor-'));
  const sibling = await fs.mkdtemp(path.join(os.tmpdir(), 'mundane-outside-'));
  try {
    await fs.writeFile(path.join(root, 'editor.json'), JSON.stringify({format:'mundane-editor-project-0.1',source:'yaml-0.3',files:['a.yaml'],attributeSchema:null}));
    await fs.writeFile(path.join(root, 'a.yaml'), '\ufeffdisk\n');
    assert.equal((await snapshot(root, 'editor.json', [])).files[0].text, '\ufeffdisk\n');
    const document = {uri:{scheme:'file',fsPath:path.join(root,'a.yaml')},getText:()=>'unsaved\n'};
    assert.equal((await snapshot(root, 'editor.json', [document])).files[0].text, 'unsaved\n');
    await fs.writeFile(path.join(root, 'a.yaml'), Buffer.from([255]));
    await assert.rejects(snapshot(root, 'editor.json', []));
    document.getText = ()=>'\ud800';
    await assert.rejects(snapshot(root, 'editor.json', [document]), /surrogate/);
    await fs.unlink(path.join(root, 'a.yaml'));
    await fs.writeFile(path.join(sibling, 'outside.yaml'), 'outside');
    await fs.symlink(path.join(sibling, 'outside.yaml'), path.join(root, 'a.yaml'));
    await assert.rejects(snapshot(root, 'editor.json', []), /outside/);
  } finally {
    await fs.rm(root,{recursive:true,force:true}); await fs.rm(sibling,{recursive:true,force:true});
  }
});
test('active processes can be cancelled and oversized output is rejected', async () => {
  const fs = require('node:fs/promises'), os = require('node:os'), path = require('node:path');
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'mundane-process-'));
  try {
    const helper = path.join(root,'bridge');
    await fs.writeFile(helper, `#!${process.execPath}\nprocess.stdin.resume();setInterval(()=>{},1000);\n`, {mode:0o755});
    const abort = new AbortController();
    const running = invoke(helper, {}, abort.signal); setTimeout(()=>abort.abort(),40);
    await assert.rejects(running,/Cancelled/);
    await fs.writeFile(helper, `#!${process.execPath}\nprocess.stdin.resume();process.stdout.write('x'.repeat(17*1024*1024));\n`);
    await assert.rejects(invoke(helper, {}), /exceeds/);
  } finally { await fs.rm(root,{recursive:true,force:true}); }
});
test('project requests share work, remain bounded and reject results after invalidation', async () => {
  const {Project} = require('../src/project');
  let reads=0,calls=0;const published=[];
  const project = new Project(async()=>{reads++;return {files:[]};},async request=>{calls++;return request;},s=>published.push(s),e=>{throw e;});
  const [a,b] = await Promise.all([project.get(),project.get()]);
  assert.equal(a,b);assert.equal(reads,1);assert.equal(calls,1);
  for(let line=1;line<=10;line++) await project.get({line});
  assert.ok(project.entries.size<=4);assert.equal(reads,1);
  project.invalidate();await project.get();assert.equal(reads,2);
  let release;const slow = new Project(async()=>({}),()=>new Promise(resolve=>release=resolve),s=>published.push(s),()=>{});
  const pending=slow.get();await new Promise(resolve=>setImmediate(resolve));const before=published.length;
  slow.invalidate();release({valid:true});assert.equal(await pending,null);assert.equal(published.length,before);
  project.dispose();assert.equal(await project.get(),null);
});
test('work selections are explicit, bounded and independent of requirement schemas', () => {
  const valid={format:'mundane-work-set-0.2',source:'mundane-work-yaml-0.2',files:['a.yaml']};
  assert.equal(project(JSON.stringify(valid),true).attributeSchema,null);
  for (const change of [{source:'yaml-0.4'},{files:[]},{files:['a.yaml','a.yaml']},{files:['../a']},{attributeSchema:null},{files:Array.from({length:129},(_,i)=>`${i}.yaml`)}]) {
    assert.throws(()=>project(JSON.stringify({...valid,...change}),true));
  }
});
test('domain overlap includes requirement declarations and clears after independent selection repair', async () => {
  const fs=require('node:fs/promises'), os=require('node:os'), path=require('node:path');
  const {snapshot}=require('../src/client');
  const root=await fs.mkdtemp(path.join(os.tmpdir(),'mundane-domain-'));
  try {
    await fs.writeFile(path.join(root,'req.json'),JSON.stringify({format:'mundane-editor-project-0.1',source:'yaml-0.4',files:['req.yaml'],attributeSchema:'shared.json'}));
    await fs.writeFile(path.join(root,'work.json'),JSON.stringify({format:'mundane-work-set-0.2',source:'mundane-work-yaml-0.2',files:['shared.json']}));
    await fs.writeFile(path.join(root,'shared.json'),'{}');await fs.writeFile(path.join(root,'req.yaml'),'source');
    await assert.rejects(snapshot(root,'req.json',[],undefined,false,'work.json'),/both/);
    await assert.rejects(snapshot(root,'work.json',[],undefined,true,'req.json'),/both/);
    await fs.writeFile(path.join(root,'work.json'),'{');
    assert.equal((await snapshot(root,'req.json',[],undefined,false,'work.json')).schema.path,'shared.json');
  } finally {await fs.rm(root,{recursive:true,force:true});}
});
test('import snapshots overlay mapped buffers, watch missing sources and preserve local work on read failures', async () => {
  const fs=require('node:fs/promises'),os=require('node:os'),path=require('node:path');
  const {snapshot}=require('../src/client'),root=await fs.mkdtemp(path.join(os.tmpdir(),'mundane-imports-'));
  try {
    const files={
      'work.json':JSON.stringify({format:'mundane-work-set-0.2',source:'mundane-work-yaml-0.2',files:['card.yaml']}),
      'card.yaml':'local\n',
      'map.json':JSON.stringify({format:'mundane-editor-imports-0.1',manifest:'imports.json',sourceRoots:{req:'.'}}),
      'imports.json':JSON.stringify({format:'mundane-imports-0.1',imports:[{scope:'req',path:'compiled.json',kind:'requirements',sha256:null,dependsOn:[]}]}),
      'compiled.json':JSON.stringify({sources:[{path:'target.yaml'}]}),'target.yaml':'disk\n'};
    for(const [name,text] of Object.entries(files))await fs.writeFile(path.join(root,name),text);
    const documents=[{uri:{scheme:'file',fsPath:path.join(root,'target.yaml')},getText:()=>'unsaved\n'}];let watched=[];
    const load=()=>snapshot(root,'work.json',documents,names=>watched=names,true,'','map.json');
    assert.equal((await load()).imports.sources[0].text,'unsaved\n');
    documents.length=0;await fs.unlink(path.join(root,'target.yaml'));
    assert.equal((await load()).imports.sources[0].text,null);assert.ok(watched.includes('target.yaml'));
    await fs.writeFile(path.join(root,'compiled.json'),'{');const failed=await load();
    assert.ok(failed.importError);assert.equal(failed.files[0].text,'local\n');assert.ok(watched.includes('compiled.json'));
    await fs.writeFile(path.join(root,'compiled.json'),files['compiled.json']);assert.ok((await load()).imports);
  } finally {await fs.rm(root,{recursive:true,force:true});}
});
