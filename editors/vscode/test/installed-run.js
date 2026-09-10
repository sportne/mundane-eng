'use strict';
const path = require('node:path');
const fs = require('node:fs/promises');
const {execFileSync} = require('node:child_process');
const {downloadAndUnzipVSCode,resolveCliArgsFromVSCodeExecutablePath,runTests} = require('@vscode/test-electron');
const metadata = require('../versions.json');
async function main() {
  const root=path.resolve(__dirname,'../../..');
  const base=path.join(root,'build/editor-installed-test');
  await fs.rm(base,{recursive:true,force:true});await fs.mkdir(base,{recursive:true});
  const bundle=execFileSync('python3',[path.join(root,'scripts/check-editor-package.py'),'--extract',path.join(base,'extracted')],{encoding:'utf8'}).trim();
  const bridge=path.join(bundle,'bin/mundane-editor');
  const workspace=path.join(base,'workspace');await fs.cp(path.join(__dirname,'fixtures'),workspace,{recursive:true});
  await fs.mkdir(path.join(workspace,'.vscode'));
  await fs.cp(path.join(root,'roadmap'),path.join(workspace,'roadmap'),{recursive:true});
  await require('./stage-imports').stage(root,workspace);
  await require('./stage-engineering').stage(root,workspace);
  await fs.writeFile(path.join(workspace,'.vscode/settings.json'),JSON.stringify({'mundane.project':'editor.json','mundane.engineeringExecutable':path.join(bundle,'bin/mundane-engineering-editor'),'mundane.executable':bridge}));
  const extensions=path.join(base,'extensions'),profile=path.join(base,'profile');
  const vscode=await downloadAndUnzipVSCode({version:'1.109.5',cachePath:path.join(root,'build/vscode-test')});
  const [cli,...args]=resolveCliArgsFromVSCodeExecutablePath(vscode,{reuseMachineInstall:true});
  const profileArgs=['--extensions-dir',extensions,'--user-data-dir',profile];
  execFileSync(cli,[...args,...profileArgs,'--no-sandbox','--install-extension',path.join(bundle,'extension',`mundane-requirements-${metadata.version}.vsix`)],{stdio:'inherit',timeout:60000,env:{...process.env,DONT_PROMPT_WSL_INSTALL:'1',VSCODE_IPC_HOOK_CLI:'',VSCODE_PORTABLE:''}});
  const harness=path.join(base,'test-harness');await fs.mkdir(harness);
  await fs.writeFile(path.join(harness,'package.json'),JSON.stringify({name:'installed-editor-test-harness',publisher:'mundane-tests',version:'0.0.1',engines:{vscode:'^1.109.0'}}));
  await runTests({vscodeExecutablePath:vscode,extensionDevelopmentPath:harness,extensionTestsPath:path.join(__dirname,'installed.js'),
    launchArgs:[workspace,...profileArgs,'--disable-workspace-trust','--no-sandbox','--disable-gpu','--skip-welcome','--skip-release-notes'],
    extensionTestsEnv:{MUNDANE_INSTALLED_EXTENSIONS:extensions,MUNDANE_INSTALLED_BRIDGE:bridge,MUNDANE_TEST_NODE:process.execPath}});
}
main().catch(error=>{console.error(error);process.exitCode=1;});
