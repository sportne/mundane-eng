'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const vscode = require('vscode');
const pause = () => new Promise(resolve=>setTimeout(resolve,350));
async function run() {
  const extension=vscode.extensions.getExtension('mundane-engineering.mundane-requirements');
  assert.ok(extension,'packaged extension installed');
  assert.ok(extension.extensionPath.startsWith(process.env.MUNDANE_INSTALLED_EXTENSIONS+path.sep),'loads installed VSIX, not development source');
  const api=await extension.activate();await pause();
  const folder=vscode.workspace.workspaceFolders[0];
  const settings=vscode.workspace.getConfiguration('mundane',folder.uri);
  const bridge=process.env.MUNDANE_INSTALLED_BRIDGE;
  assert.equal(settings.get('executable'),bridge);
  assert.ok(bridge.includes('/extracted/'),'uses extracted bridge');
  assert.equal((await api.validate())[0].result.valid,true);
  const selection=vscode.Uri.joinPath(folder.uri,'editor.json');
  async function broken(executable,marker) {
    await settings.update('executable',executable,vscode.ConfigurationTarget.WorkspaceFolder);await pause();
    assert.equal((await api.validate()).length,0);
    assert.ok(vscode.languages.getDiagnostics(selection).some(d=>d.code==='editor-configuration' && d.message.includes(marker)));
    await settings.update('executable',bridge,vscode.ConfigurationTarget.WorkspaceFolder);await pause();
    assert.equal((await api.validate())[0].result.valid,true);
    assert.equal(vscode.languages.getDiagnostics(selection).length,0);
  }
  await broken(path.join(folder.uri.fsPath,'absent-bridge'),'ENOENT');
  const mismatch=path.join(folder.uri.fsPath,'incompatible-bridge');
  await fs.writeFile(mismatch,`#!${process.env.MUNDANE_TEST_NODE}\nprocess.stdin.resume();process.stdin.on('end',()=>process.stdout.write(JSON.stringify({protocol:'unsupported'})));\n`,{mode:0o755});
  await broken(mismatch,'Unsupported editor response');
  const document=await vscode.workspace.openTextDocument(selection),original=document.getText();
  async function replace(text) {
    const edit=new vscode.WorkspaceEdit();edit.replace(selection,new vscode.Range(document.positionAt(0),document.positionAt(document.getText().length)),text);
    assert.ok(await vscode.workspace.applyEdit(edit));
  }
  await replace('{"format":"unsupported"}');await api.validate();
  assert.ok(vscode.languages.getDiagnostics(selection).some(d=>d.code==='editor-configuration'));
  await replace(original);assert.equal((await api.validate())[0].result.valid,true);
  assert.equal(vscode.languages.getDiagnostics(selection).length,0);
  await require('./host').run();
  console.log('PASS installed VSIX and extracted bridge: isolated profile/workspace, configuration/protocol failure and recovery, full provider workflow');
}
module.exports={run};
