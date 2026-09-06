'use strict';
const fs=require('node:fs/promises'),path=require('node:path'),{execFileSync}=require('node:child_process'),{createHash}=require('node:crypto');
async function stage(root,workspace) {
  const cp=path.join(root,'build/maintained/classes')+':'+path.join(root,'build/dependencies/snakeyaml-engine-3.1.1.jar');
  const compile=(main,args)=>execFileSync('java',['-cp',cp,main,...args],{cwd:workspace,timeout:30000});
  await fs.mkdir(path.join(workspace,'artifacts'),{recursive:true});
  const requirements=compile('mundanereq.cli.CompileMain',['--source=yaml-0.3','--root','.', 'imported/req.mreq.yaml']);
  const work=compile('engineering.work.WorkMain',['compile','--root','.','imported/work.json']);
  await fs.writeFile(path.join(workspace,'artifacts/requirements.json'),requirements);
  await fs.writeFile(path.join(workspace,'artifacts/work.json'),work);
  for(const base of ['mapped','snapshot'])await fs.cp(path.join(workspace,'imported'),path.join(workspace,base,'imported'),{recursive:true});
  const file=path.join(workspace,'imports.json'),manifest=JSON.parse(await fs.readFile(file,'utf8'));
  manifest.imports[0].sha256=createHash('sha256').update(requirements).digest('hex');
  await fs.writeFile(file,JSON.stringify(manifest));
}
module.exports={stage};
