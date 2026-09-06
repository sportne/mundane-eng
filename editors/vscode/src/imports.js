'use strict';

// Select bytes only. The bridge owns strict JSON, artifact and pin validation.
async function load(root, selectionPath, documents, watch, read, relative) {
  let total=0;
  async function take(name,maximum,optional=false) {
    relative(name);watch(name);
    let file;
    try {file=await read(root,name,documents,maximum);} catch(error) {if(optional)return {path:name,text:null};throw error;}
    total+=Buffer.byteLength(file.text);
    if(total>16*1024*1024)throw new Error('Imported snapshots exceed 16 MiB');
    return file;
  }
  const selection=await take(selectionPath,65536),config=JSON.parse(selection.text);
  if(config.format!=='mundane-editor-imports-0.1'||!config.sourceRoots||typeof config.sourceRoots!=='object'||Array.isArray(config.sourceRoots))throw new Error('Invalid editor import selection');
  const manifest=await take(relative(config.manifest),65536),declaration=JSON.parse(manifest.text);
  if(declaration.format!=='mundane-imports-0.1'||!Array.isArray(declaration.imports)||declaration.imports.length>100)throw new Error('Invalid bounded import manifest');
  const artifacts=new Map(), sourceNames=new Set();
  for(const entry of declaration.imports) {
    if(!['requirements','work-items'].includes(entry.kind))throw new Error('Editor imports support requirements and YAML work items');
    const prefix=config.sourceRoots[entry.scope];
    if(prefix!=='.')relative(prefix);
    const name=relative(entry.path);
    if(!artifacts.has(name))artifacts.set(name,await take(name,16*1024*1024));
    const artifact=JSON.parse(artifacts.get(name).text);
    if(!Array.isArray(artifact.sources))throw new Error('Compiled import has no source inventory');
    for(const source of artifact.sources) {
      const name=relative(prefix==='.'?relative(source.path):prefix+'/'+relative(source.path));
      sourceNames.add(name);if(sourceNames.size>256)throw new Error('Select at most 256 mapped source files');
    }
  }
  const sources=[];for(const name of [...sourceNames].sort())sources.push(await take(name,8*1024*1024,true));
  return {selection,manifest,artifacts:[...artifacts.values()],sources};
}
module.exports={load};
