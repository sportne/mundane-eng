'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs/promises'),path=require('node:path'),vscode=require('vscode');
async function run(api) {
  const folder=vscode.workspace.workspaceFolders[0],settings=vscode.workspace.getConfiguration('mundane',folder.uri);
  await settings.update('engineeringProject','engineering-editor.json',vscode.ConfigurationTarget.WorkspaceFolder);
  const open=name=>vscode.workspace.openTextDocument(vscode.Uri.joinPath(folder.uri,name));
  const check=async()=>{for(let i=0;i<20;i++){const states=await api.validate();const state=states.find(s=>s.snapshot.source==='mundane-engineering-editor-0.1');if(state)return state;await new Promise(r=>setTimeout(r,100));}assert.fail('engineering snapshot unavailable: '+JSON.stringify(vscode.languages.getDiagnostics().map(([u,d])=>[u.fsPath,d.map(x=>x.message)])));};
  let state=await check();assert.equal(state.result.valid,true,JSON.stringify(state.result.diagnostics));
  assert.equal(state.snapshot.files.length,12);
  const procedure=await open('procedure-stale.yaml'),budget=await open('budget.yaml');
  await vscode.languages.setTextDocumentLanguage(procedure,'yaml');await vscode.languages.setTextDocumentLanguage(budget,'yaml');
  const original=procedure.getText(),power=budget.getText();
  async function replace(doc,text){const edit=new vscode.WorkspaceEdit();edit.replace(doc.uri,new vscode.Range(doc.positionAt(0),doc.positionAt(doc.getText().length)),text);assert.ok(await vscode.workspace.applyEdit(edit));}
  await replace(procedure,original.replace(/withinMs: [0-9]+/,'withinMs: invalid'));state=await check();assert.ok(state.result.diagnostics.some(d=>d.path==='procedure-stale.yaml'&&d.code==='engineering-schema'));
  await replace(procedure,original);await replace(budget,power.replace(/max: [0-9]+/,'max: invalid'));assert.equal((await check()).result.valid,false);
  await replace(budget,power);assert.equal((await check()).result.valid,true);
  const position=procedure.positionAt(original.indexOf('withinMs:')+'withinMs: '.length);
  const hovers=await vscode.commands.executeCommand('vscode.executeHoverProvider',procedure.uri,position);
  assert.ok(hovers.some(h=>h.contents.some(c=>c.value.includes('Working'))));
  state=await check();const imported=state.result.importNavigation.find(n=>n.reference.path==='procedure-stale.yaml');assert.ok(imported,'pinned imported origin');
  const point=new vscode.Position(imported.reference.start.line-1,imported.reference.start.column);
  const definitions=await vscode.commands.executeCommand('vscode.executeDefinitionProvider',procedure.uri,point);
  assert.equal(definitions.length,1);assert.equal(definitions[0].uri.fsPath,path.join(folder.uri.fsPath,imported.target.path));
  const completions=await vscode.commands.executeCommand('vscode.executeCompletionItemProvider',procedure.uri,point);
  assert.ok(completions.items.length>0,'typed imported completions');
  const edits=await vscode.commands.executeCommand('vscode.executeFormatDocumentProvider',procedure.uri,{tabSize:2,insertSpaces:true});
  assert.equal((edits || []).length,0);assert.equal(procedure.getText(),original,'unsupported formatting preserves bytes');
  const selected=state.snapshot.files.find(f=>f.path==='procedure-stale.yaml').imports.find(i=>i.kind==='architecture');
  const importedFile=path.join(folder.uri.fsPath,selected.path),bytes=await fs.readFile(importedFile);
  await fs.unlink(importedFile);assert.equal((await check()).result.valid,false);
  await fs.writeFile(importedFile,bytes);assert.equal((await check()).result.valid,true);
  // Rapid unsaved edits must not publish the obsolete invalid response.
  await replace(procedure,original.replace(/withinMs: [0-9]+/,'withinMs: invalid'));const pending=api.validate();
  await replace(procedure,original);await pending;assert.equal((await check()).result.valid,true);
  const source=await open(imported.target.path),sourceText=source.getText();
  await replace(source,'# changed working copy\n'+sourceText);
  state=await check();assert.ok(!state.result.importNavigation.some(n=>n.reference.path==='procedure-stale.yaml'&&n.target.path===imported.target.path),'dirty origin cannot masquerade as pinned source');
  await replace(source,sourceText);assert.equal((await check()).result.valid,true);
  assert.equal(await fs.readFile(procedure.uri.fsPath,'utf8'),original);
  await settings.update('engineeringViews',['change.md'],vscode.ConfigurationTarget.WorkspaceFolder);
  const view=await vscode.commands.executeCommand('mundane.openEngineeringView');assert.equal(view.fsPath,path.join(folder.uri.fsPath,'change.md'));
  await settings.update('engineeringViews',[],vscode.ConfigurationTarget.WorkspaceFolder);
  await settings.update('engineeringProject','',vscode.ConfigurationTarget.WorkspaceFolder);await api.validate();
  console.log('PASS engineering installed providers: twelve families, unsaved timing/power, typed completion/hover/import navigation, dirty revision suppression, unavailable import recovery, stale request rejection and preserved formatting');
}
module.exports={run};
