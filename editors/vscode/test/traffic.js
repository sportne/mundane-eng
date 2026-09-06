'use strict';
const vscode = require('vscode');
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const pause = () => new Promise(resolve => setTimeout(resolve, 500));
async function run() {
  const extension = vscode.extensions.getExtension('mundane-engineering.mundane-requirements');
  const client = require(path.join(extension.extensionPath,'src/client.js'));
  const original = client.invoke; let counts = {one:0,two:0};
  client.invoke = (...args) => { counts[args[1].files.some(f=>f.text.includes('SECOND'))?'two':'one']++; return original(...args); };
  const api = await extension.activate();
  const [one,two] = vscode.workspace.workspaceFolders;
  const document = await vscode.workspace.openTextDocument(vscode.Uri.joinPath(one.uri,'parent.mreq.yaml'));
  const other = await vscode.workspace.openTextDocument(vscode.Uri.joinPath(two.uri,'parent.mreq.yaml'));
  const notes = await vscode.workspace.openTextDocument(vscode.Uri.joinPath(one.uri,'notes.txt'));
  await pause(); await api.validate(); counts={one:0,two:0};
  const observations={};
  for(let i=0;i<6;i++) await api.current(document,undefined,new vscode.Position(4,15));
  observations.repeatedQueries={...counts}; counts={one:0,two:0};
  const edit=new vscode.WorkspaceEdit();edit.insert(notes.uri,new vscode.Position(0,0),'Unrelated edit. ');await vscode.workspace.applyEdit(edit);await pause();
  observations.unrelatedEdit={...counts};counts={one:0,two:0};
  const update=new vscode.WorkspaceEdit();update.insert(document.uri,new vscode.Position(0,0),'# Changed requirement file\n');await vscode.workspace.applyEdit(update);await pause();
  observations.selectedEdit={...counts};counts={one:0,two:0};
  const concurrent=await Promise.all([api.current(document,undefined,new vscode.Position(5,15)),api.current(other,undefined,new vscode.Position(4,15))]);
  observations.concurrentFolders={...counts,results:concurrent.filter(Boolean).length};
  await fs.writeFile(process.env.MUNDANE_TRAFFIC_OUTPUT,JSON.stringify(observations,null,2)+'\n');
  if(process.env.MUNDANE_TRAFFIC_ASSERT==='1') {
    assert.deepEqual(observations.repeatedQueries,{one:1,two:0});
    assert.deepEqual(observations.unrelatedEdit,{one:0,two:0});
    assert.deepEqual(observations.selectedEdit,{one:1,two:0});
    assert.equal(observations.concurrentFolders.results,2);
  }
  console.log('PASS measured editor request traffic '+JSON.stringify(observations));
}
module.exports={run};
