'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vscode = require('vscode');
async function run(api) {
  const folder = vscode.workspace.workspaceFolders[0];
  const settings = vscode.workspace.getConfiguration('mundane',folder.uri);
  await settings.update('workProject','work.json',vscode.ConfigurationTarget.WorkspaceFolder);
  const open = name => vscode.workspace.openTextDocument(vscode.Uri.joinPath(folder.uri,name));
  const a = await open('alpha.mwork.yaml'), b = await open('beta.mwork.yaml');
  assert.equal(a.languageId,'mundane-work-items');
  const originalA = a.getText(), originalB = b.getText();
  async function replace(doc,text) {
    const edit = new vscode.WorkspaceEdit();
    edit.replace(doc.uri,new vscode.Range(doc.positionAt(0),doc.positionAt(doc.getText().length)),text);
    assert.ok(await vscode.workspace.applyEdit(edit));
  }
  assert.equal((await api.validate()).filter(x=>x.result.valid).length,2,'requirements and work validate together');
  for (const [text,code] of [
    [originalB.replace('Planned','Wrong'),'invalid-work-source'],
    [originalB.replace('id: BETA','id: ALPHA'),'duplicate-work-id'],
    [originalB.replace('[ALPHA]','[MISSING]'),'missing-work-target'],
    [originalB.replace('[ALPHA]','[BETA]'),'dependency-cycle']
  ]) {
    await replace(b,text); await api.validate();
    assert.ok(vscode.languages.getDiagnostics().some(([,ds])=>ds.some(d=>d.code===code)),code);
    await replace(b,originalB);assert.equal((await api.validate()).filter(x=>x.result.valid).length,2);
    assert.equal(vscode.languages.getDiagnostics(b.uri).length,0);
  }
  const references = () => vscode.commands.executeCommand('vscode.executeDefinitionProvider',b.uri,b.positionAt(b.getText().indexOf('[ALPHA]')+2));
  let definitions=await references();assert.equal(definitions.length,1);
  assert.equal(definitions[0].uri.fsPath,a.uri.fsPath);assert.equal(a.getText(definitions[0].range),'ALPHA');
  const firstLine=definitions[0].range.start.line;
  await replace(a,'\n'+originalA);definitions=await references();assert.equal(definitions[0].range.start.line,firstLine+1);
  await replace(a,'{format: mundane-work-yaml-0.2, title: "😀 title", id: ALPHA, kind: task, status: Planned, body: Text}\n');
  definitions=await references();assert.equal(a.getText(definitions[0].range),'ALPHA','Unicode offsets target exact ID');
  await replace(a,originalA);
  assert.equal((await vscode.commands.executeCommand('vscode.executeDefinitionProvider',b.uri,b.positionAt(b.getText().lastIndexOf('ALPHA')+1))).length,0,'body is not navigation');
  await replace(b,originalB.replace('id: BETA','id: ALPHA'));assert.equal((await references()).length,0);
  await replace(b,originalB);
  assert.equal(((await vscode.commands.executeCommand('vscode.executeFormatDocumentProvider',b.uri,{tabSize:2,insertSpaces:true}))||[]).length,0,'work items have no formatter');
  console.log('PASS work navigation: structural dependencies, unsaved locations, Unicode targets, ambiguity and prose exclusion');
  const pending = api.validate();await replace(b,originalB.replace('Planned','Wrong'));await pending;
  await api.validate();assert.ok(vscode.languages.getDiagnostics(b.uri).length);
  await replace(b,originalB);await api.validate();
  const selection=await open('work.json'), selectionText=selection.getText();
  await replace(selection,'{');assert.equal((await api.validate()).length,1,'broken work selection leaves requirements healthy');
  await replace(selection,selectionText);assert.equal((await api.validate()).length,2);
  assert.equal(fs.readFileSync(b.uri.fsPath,'utf8'),originalB,'unsaved checks never save');
  assert.equal(a.getText(),originalA);
  console.log('PASS work editor: mixed domains, unsaved fields/duplicates/dependencies/cycles, stale results and selection recovery');
  await settings.update('workProject','',vscode.ConfigurationTarget.WorkspaceFolder);await api.validate();
}
module.exports={run};
