'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { project, relative, invoke } = require('../src/client');
test('explicit project rejects traversal, ambiguity and unsupported profiles', () => {
  for (const p of ['../x', '/x', 'a//b', 'C:/file', 'a/./b']) assert.throws(() => relative(p));
  const valid = { format: 'mundane-editor-project-0.1', source: 'yaml-0.3', files: ['a.yaml'], attributeSchema: null };
  assert.equal(project(JSON.stringify(valid)).source, 'yaml-0.3');
  for (const changes of [{files:[]}, {files:['a.yaml','a.yaml']}, {source:'unknown'}, {attributeSchema:'schema.json'}, {extra:true}]) assert.throws(() => project(JSON.stringify({...valid,...changes})));
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
