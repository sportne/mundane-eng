'use strict';
const fs=require('node:fs/promises'),path=require('node:path');
async function stage(root,workspace) {
  // The existing work-import fixture owns the root imports.json selection.
  const existing=await fs.readFile(path.join(workspace,'imports.json')).catch(()=>null);
  await fs.cp(path.join(root,'build/gcs-change'),workspace,{recursive:true});
  if(existing)await fs.writeFile(path.join(workspace,'imports.json'),existing);
  const names=['architecture','configuration-sim','safety-software','procedure-stale','equipment','budget','software','waived','operations','change'];
  const files=[];
  for(const name of names){
    const artifact=JSON.parse(await fs.readFile(path.join(workspace,name+'.json'),'utf8'));
    const imports='editor-'+name+'-imports.json';
    await fs.writeFile(path.join(workspace,imports),JSON.stringify({format:'mundane-domain-imports-0.1',imports:artifact.imports}));
    files.push({path:name+'.yaml',kind:artifact.artifactKind,imports});
  }
  await fs.copyFile(path.join(root,'build/gcs-evidence/assessment.yaml'),path.join(workspace,'assessment.yaml'));
  files.push({path:'assessment.yaml',kind:'assessment',imports:null});
  await fs.copyFile(path.join(root,'examples/ground-control-station/engineering/manual-inspection.yaml'),path.join(workspace,'manual-inspection.yaml'));
  files.push({path:'manual-inspection.yaml',kind:'manual-observation',imports:null});
  await fs.writeFile(path.join(workspace,'engineering-editor.json'),JSON.stringify({format:'mundane-engineering-editor-project-0.1',files}));
}
module.exports={stage};
