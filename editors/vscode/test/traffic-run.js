const path=require('node:path'),fs=require('node:fs/promises');
const root=path.resolve(__dirname,'../../..');
const {runTests}=require('@vscode/test-electron');
(async()=>{
 const base=root+'/build/editor-traffic-workspace';await fs.rm(base,{recursive:true,force:true});await fs.mkdir(base,{recursive:true});
 for(const name of ['one','two']) {
  const folder=base+'/'+name;await fs.cp(root+'/editors/vscode/test/fixtures',folder,{recursive:true});await fs.mkdir(folder+'/.vscode');
  await fs.writeFile(folder+'/notes.txt','Notes\n');
  if(name==='two') for(const file of ['parent.mreq.yaml','child.mreq.yaml']) await fs.writeFile(folder+'/'+file,(await fs.readFile(folder+'/'+file,'utf8')).replaceAll('PARENT','SECOND-PARENT'));
  await fs.writeFile(folder+'/.vscode/settings.json',JSON.stringify({'mundane.project':'editor.json','mundane.executable':root+'/build/maintained/mundane-editor'}));
 }
 const ws=base+'/traffic.code-workspace';await fs.writeFile(ws,JSON.stringify({folders:[{path:'one'},{path:'two'}]}));
 await runTests({version:'1.109.5',extensionDevelopmentPath:root+'/editors/vscode',extensionTestsPath:root+'/editors/vscode/test/traffic.js',cachePath:root+'/build/vscode-test',launchArgs:[ws,'--disable-workspace-trust','--no-sandbox','--disable-gpu'],extensionTestsEnv:{MUNDANE_TRAFFIC_OUTPUT:process.env.MUNDANE_TRAFFIC_OUTPUT||root+'/build/editor-traffic-current.json',MUNDANE_TRAFFIC_ASSERT:process.env.MUNDANE_TRAFFIC_ASSERT||'1'}});
})().catch(e=>{console.error(e);process.exitCode=1});
