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
