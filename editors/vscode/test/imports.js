'use strict';
const assert=require('node:assert/strict'),vscode=require('vscode');
async function run(api) {
  const folder=vscode.workspace.workspaceFolders[0],settings=vscode.workspace.getConfiguration('mundane',folder.uri);
  const open=name=>vscode.workspace.openTextDocument(vscode.Uri.joinPath(folder.uri,name));
  const b=await open('beta.mwork.yaml'),req=await open('mapped/imported/req.mreq.yaml'),other=await open('imported/task.mwork.yaml');
  const original=b.getText(),reqText=req.getText(),otherText=other.getText();
  async function replace(doc,text) {
    const edit=new vscode.WorkspaceEdit();edit.replace(doc.uri,new vscode.Range(doc.positionAt(0),doc.positionAt(doc.getText().length)),text);
    assert.ok(await vscode.workspace.applyEdit(edit));
  }
  await settings.update('workProject','work.json',vscode.ConfigurationTarget.WorkspaceFolder);
  await settings.update('workImports','editor-imports.json',vscode.ConfigurationTarget.WorkspaceFolder);
  const related=scope=>original+`relations:\n- {relation: addresses, scope: ${scope}, kind: ${scope==='other'?'work-item':'requirement'}, target: SHARE}\n`;
  const definition=()=>vscode.commands.executeCommand('vscode.executeDefinitionProvider',b.uri,b.positionAt(b.getText().lastIndexOf('SHARE')+2));
  await replace(b,related('req'));let states=await api.validate();assert.equal(states.length,2);
  let work=states.find(s=>s.snapshot.source==='mundane-work-yaml-0.2');assert.equal(work.result.importDiagnostics.length,0);
  let target=await definition();assert.equal(target.length,1);assert.equal(target[0].uri.fsPath,req.uri.fsPath);assert.equal(req.getText(target[0].range),'"SHARE"');
  assert.equal(await api.current(req),null,'imported source is not treated as an authored work file');
  await replace(req,'# unsaved comment\n'+reqText);await api.validate();assert.equal((await definition()).length,0);
  assert.ok(vscode.languages.getDiagnostics(b.uri).some(d=>d.code==='editor-import-source'&&d.severity===vscode.DiagnosticSeverity.Warning));
  assert.equal(b.getText(vscode.languages.getDiagnostics(b.uri).find(d=>d.code==='editor-import-source').range),'SHARE');
  await replace(b,related('baseline'));target=await definition();assert.equal(target.length,1);assert.ok(target[0].uri.fsPath.endsWith('/snapshot/imported/req.mreq.yaml'),'same ID resolves only within selected scope');
  await replace(req,reqText);await replace(b,related('other'));target=await definition();assert.equal(target.length,1);assert.equal(other.getText(target[0].range),'SHARE');
  await replace(other,otherText.replace('Planned','Complete'));assert.equal((await definition()).length,0);await replace(other,otherText);
  await replace(b,related('missing'));await api.validate();assert.equal((await definition()).length,0);
  assert.ok(vscode.languages.getDiagnostics(b.uri).some(d=>d.code==='editor-import-target'));
  const manifest=await open('imports.json'),manifestText=manifest.getText();
  await replace(b,related('req'));const bad=JSON.parse(manifestText);bad.imports[0].sha256='0'.repeat(64);await replace(manifest,JSON.stringify(bad));await api.validate();
  assert.equal((await definition()).length,0);assert.ok(vscode.languages.getDiagnostics(vscode.Uri.joinPath(folder.uri,'editor-imports.json')).some(d=>d.code==='digest-mismatch'));
  const local=await vscode.commands.executeCommand('vscode.executeDefinitionProvider',b.uri,b.positionAt(b.getText().indexOf('[ALPHA]')+2));assert.equal(local.length,1,'invalid imports preserve local navigation');
  await replace(manifest,manifestText);assert.equal((await definition()).length,1);
  assert.equal((await vscode.commands.executeCommand('vscode.executeDefinitionProvider',b.uri,b.positionAt(b.getText().indexOf('Literal ALPHA')+10))).length,0);
  await replace(b,original);await settings.update('workImports','',vscode.ConfigurationTarget.WorkspaceFolder);
  await settings.update('workProject','',vscode.ConfigurationTarget.WorkspaceFolder);await api.validate();
  console.log('PASS imported navigation: requirement/work targets, mapped Unicode origins, unsaved changes, scope isolation, pin failure and local recovery');
}
module.exports={run};
