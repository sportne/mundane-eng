'use strict';
const client=require('./client');
function project(text) {
  const p=JSON.parse(text);
  if(!p || Object.keys(p).sort().join()!=='files,format' || p.format!=='mundane-engineering-editor-project-0.1' || !Array.isArray(p.files) || p.files.length<1 || p.files.length>128)throw new Error('Invalid engineering selection');
  const kinds=['architecture','configuration','safety','procedure','assessment','manual-observation','equipment','budget','software','assurance','operations','change'];
  const seen=new Set();
  for(const f of p.files){
    if(Object.keys(f).sort().join()!=='imports,kind,path'||!kinds.includes(f.kind))throw new Error('Unsupported engineering selection entry');
    client.relative(f.path);if(seen.has(f.path))throw new Error('Duplicate engineering source');seen.add(f.path);
    if(f.imports!==null)client.relative(f.imports);
  }
  return p;
}
async function snapshot(root,selection,documents,selected=()=>{},others=[]) {
  const config=project((await client.read(root,selection,documents,65536)).text);
  const watched=new Set(config.files.map(f=>f.path));const watch=p=>{watched.add(p);selected([...watched]);};selected([...watched]);
  for(const name of others.filter(Boolean)){
    let other;try{other=JSON.parse((await client.read(root,name,documents,65536)).text);}catch(_){continue;}
    if([...other.files||[],other.attributeSchema].some(p=>watched.has(p)))throw new Error('A source is selected in multiple authoring domains');
  }
  const files=[],artifacts=new Map(),sources=new Map();
  for(const entry of config.files){
    const file={...await client.read(root,entry.path,documents,1048576),kind:entry.kind,imports:[],importError:null};
    if(entry.imports!==null)try{
      watch(entry.imports);
      const imports=JSON.parse((await client.read(root,entry.imports,documents,1048576)).text);
      if(Object.keys(imports).sort().join()!=='format,imports'||imports.format!=='mundane-domain-imports-0.1'||!Array.isArray(imports.imports)||imports.imports.length>128)throw new Error('Invalid domain imports selection');
      file.imports=imports.imports;
      for(const i of imports.imports){
        client.relative(i.path);watch(i.path);
        const artifact=await client.read(root,i.path,documents,8388608);artifacts.set(i.path,artifact);
        const compiled=JSON.parse(artifact.text);
        for(const source of compiled.sources||[]){
          client.relative(source.path);watch(source.path);
          try{sources.set(source.path,await client.read(root,source.path,documents,1048576));}catch(_){/* Missing origins disable navigation, not compiled indexes. */}
        }
      }
    }catch(e){file.importError=String(e.message).slice(0,1024);}
    files.push(file);
  }
  return {protocol:client.PROTOCOL,source:'mundane-engineering-editor-0.1',files,schema:null,imports:{artifacts:[...artifacts.values()],sources:[...sources.values()]},cursor:null};
}
module.exports={project,snapshot};
