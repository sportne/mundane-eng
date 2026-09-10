'use strict';
const { spawn } = require('node:child_process');
const path = require('node:path');
const fs = require('node:fs/promises');
const LIMIT = 16 * 1024 * 1024;
const metadata = require('../versions.json');
const PROTOCOL = metadata.protocol;

function relative(name) {
  if (typeof name !== 'string' || !name || /[\\:]/.test(name) || name.split('/').some(p => !p || p === '.' || p === '..')) {
    throw new Error('Select normalized project-relative paths');
  }
  return name;
}
function project(text, work = false) {
  const p = JSON.parse(text);
  if (work) {
    if (!p || Object.keys(p).sort().join() !== 'files,format,source' || p.format !== 'mundane-work-set-0.2' || p.source !== 'mundane-work-yaml-0.2') throw new Error('Invalid work-item editor selection');
    if (!Array.isArray(p.files) || p.files.length < 1 || p.files.length > 128) throw new Error('Select 1–128 work-item files');
    p.files.forEach(relative);
    if (new Set(p.files).size !== p.files.length) throw new Error('Duplicate selected path');
    return {...p, attributeSchema:null};
  }
  if (!p || Object.keys(p).sort().join() !== 'attributeSchema,files,format,source' || p.format !== metadata.project || !['yaml-0.3', 'yaml-0.4'].includes(p.source)) throw new Error('Invalid editor project contract');
  if (!Array.isArray(p.files) || p.files.length < 1 || p.files.length > 128) throw new Error('Select 1–128 requirement files');
  p.files.forEach(relative);
  if (new Set(p.files).size !== p.files.length) throw new Error('Duplicate selected path');
  if (p.attributeSchema !== null) {
    relative(p.attributeSchema);
    if (p.source !== 'yaml-0.4' || p.files.includes(p.attributeSchema)) throw new Error('Invalid attribute schema selection');
  }
  return p;
}
async function read(root, name, documents, maximum) {
  relative(name);
  const file = path.join(root, name);
  const real = await fs.realpath(file);
  const realRoot = await fs.realpath(root);
  const inside = path.relative(realRoot, real);
  if (inside.startsWith('..' + path.sep) || inside === '..' || path.isAbsolute(inside)) throw new Error('Selected file resolves outside workspace');
  const stat = await fs.stat(file);
  if (!stat.isFile() || stat.size > maximum) throw new Error('Selected file is not a bounded regular file');
  const document = documents.find(d => d.uri.scheme === 'file' && d.uri.fsPath === file);
  let text;
  if (document) text = document.getText();
  else {
    const handle = await fs.open(file, 'r');
    try {
      const buffer = Buffer.alloc(maximum + 1);
      let count = 0;
      while (count < buffer.length) {
        const result = await handle.read(buffer, count, buffer.length - count, null);
        if (!result.bytesRead) break;
        count += result.bytesRead;
      }
      if (count > maximum) throw new Error('Selected file exceeds limit');
      text = new TextDecoder('utf-8', { fatal: true, ignoreBOM: true }).decode(buffer.subarray(0, count));
    } finally { await handle.close(); }
  }
  if (!text.isWellFormed()) throw new Error('Buffer contains an unpaired Unicode surrogate');
  if (Buffer.byteLength(text) > maximum) throw new Error('Buffer exceeds limit');
  return { path: name, text };
}
async function snapshot(root, selection, documents, selected = () => {}, work = false, otherSelection = '', importSelection = '') {
  const config = project((await read(root, selection, documents, 64 * 1024)).text, work);
  const watched=new Set([...config.files, ...(config.attributeSchema ? [config.attributeSchema] : [])]);
  const watch=name=>{watched.add(name);selected([...watched]);};
  selected([...watched]);
  if (otherSelection) {
    // Only overlapping selections are shared configuration errors. A malformed
    // independent selection must not disable the healthy domain.
    let other;
    try { other = project((await read(root, otherSelection, documents, 64 * 1024)).text, !work); } catch (_) { /* independently diagnosed */ }
    const ownPaths = [...config.files, ...(config.attributeSchema ? [config.attributeSchema] : [])];
    const otherPaths = other ? [...other.files, ...(other.attributeSchema ? [other.attributeSchema] : [])] : [];
    if (otherPaths.some(name => ownPaths.includes(name))) throw new Error('A file is selected as both requirements and work items');
  }
  const files = []; let total = 0;
  for (const name of config.files) {
    const file = await read(root, name, documents, (work ? 1 : 8) * 1024 * 1024);
    total += Buffer.byteLength(file.text);
    if (total > LIMIT) throw new Error('Snapshot exceeds 16 MiB');
    files.push(file);
  }
  const schema = config.attributeSchema === null ? null : await read(root, config.attributeSchema, documents, 1024 * 1024);
  const request={ protocol: PROTOCOL, source: config.source, files, schema };
  if(work && importSelection) {
    relative(importSelection);
    try {request.imports=await require('./imports').load(root,importSelection,documents,watch,read,relative);}
    catch(error) {request.importError={path:importSelection,message:Array.from(error.message).slice(0,1024).join('')};}
  }
  return request;
}
function invoke(executable, request, signal) {
  if (!path.isAbsolute(executable)) return Promise.reject(new Error('Configure an absolute mundane.executable path'));
  const bytes = Buffer.from(JSON.stringify(request));
  if (bytes.length > LIMIT) return Promise.reject(new Error('Snapshot exceeds 16 MiB'));
  return new Promise((resolve, reject) => {
    if (signal?.aborted) return reject(new Error('Cancelled'));
    const child = spawn(executable, [], { shell: false, stdio: ['pipe', 'pipe', 'pipe'] });
    let output = [], size = 0, errors = '', settled = false;
    const finish = (error, result) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer); signal?.removeEventListener('abort', cancel);
      if (error) { child.kill(); reject(error); } else resolve(result);
    };
    const cancel = () => finish(new Error('Cancelled'));
    const timer = setTimeout(() => finish(new Error('Compiler timed out after 15 seconds')), 15000);
    signal?.addEventListener('abort', cancel, { once: true });
    child.on('error', error => finish(error));
    child.stdin.on('error', error => finish(error));
    child.stdout.on('data', data => {
      size += data.length;
      if (size > LIMIT) return finish(new Error('Compiler response exceeds 16 MiB'));
      output.push(data);
    });
    child.stderr.on('data', data => { errors = (errors + data.toString()).slice(0, 4096); });
    child.on('close', code => {
      if (code !== 0) return finish(new Error(errors || `Compiler exited ${code}`));
      try {
        const result = JSON.parse(Buffer.concat(output).toString('utf8'));
        if (result.protocol !== PROTOCOL) throw new Error('Unsupported editor response');
        finish(null, result);
      } catch (error) { finish(error); }
    });
    child.once('spawn', () => { if (!settled) child.stdin.end(bytes); });
  });
}
module.exports = { project, snapshot, invoke, relative, read, PROTOCOL };
