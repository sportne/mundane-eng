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
  const complete = point => vscode.commands.executeCommand('vscode.executeCompletionItemProvider',b.uri,point);
  const workItems = result => result.items.filter(i=>i.detail?.startsWith('work-item:'));
  await replace(b,originalB.replace('status: Planned','status: '));
  let choices=workItems(await complete(b.positionAt(b.getText().indexOf('status: ')+8)));
  assert.deepEqual(choices.map(i=>i.label).sort(),['Complete','Conditional','In progress','Planned','Ready','Superseded']);
  const planned=choices.find(i=>i.label==='Planned');const insert=new vscode.WorkspaceEdit();insert.replace(b.uri,planned.range,planned.insertText);
  assert.ok(await vscode.workspace.applyEdit(insert));assert.equal((await api.validate()).filter(x=>x.result.valid).length,2);
  await replace(b,originalB.replace('kind: task','kind: issue').replace('dependencies: [ALPHA]\n',''));
  choices=workItems(await complete(b.positionAt(b.getText().indexOf('Planned')+2)));
  assert.deepEqual(choices.map(i=>i.label).sort(),['Closed','Open','Superseded']);
  await replace(b,originalB.replace('[ALPHA]','[AL]'));
  choices=workItems(await complete(b.positionAt(b.getText().indexOf('[AL]')+3)));
  assert.deepEqual(choices.map(i=>i.label),['ALPHA']);assert.equal(choices[0].insertText,'"ALPHA"');
  await replace(b,originalB.replace('[ALPHA]','[ALPHA, AL]'));
  assert.equal(workItems(await complete(b.positionAt(b.getText().indexOf(', AL]')+4))).length,0,'already used prerequisites excluded');
  const withRelation=originalB+'relations:\n- {relation: relates-to, scope: work, kind: work-item, target: ALPHA}\n';
  await replace(b,withRelation);
  choices=workItems(await complete(b.positionAt(b.getText().indexOf('relates-to')+2)));
  assert.deepEqual(choices.map(i=>i.label).sort(),['addresses','evidence','relates-to','supersedes']);
  choices=workItems(await complete(b.positionAt(b.getText().lastIndexOf('ALPHA')+2)));
  assert.deepEqual(choices.map(i=>i.label),['ALPHA']);
  await replace(b,withRelation.replace('scope: work','scope: elsewhere'));
  assert.equal(workItems(await complete(b.positionAt(b.getText().lastIndexOf('ALPHA')+2))).length,0,'imported scope not guessed');
  await replace(b,originalB);
  let help=await vscode.commands.executeCommand('vscode.executeHoverProvider',b.uri,b.positionAt(b.getText().indexOf('status:')+2));
  assert.equal(help.length,1);assert.ok(help[0].contents[0].value.replaceAll('&nbsp;',' ').includes('Authored task status'),JSON.stringify(help[0].contents[0].value));
  await replace(a,originalA.replace('Alpha task','"[run](command:bad) <script>literal</script>"'));
  help=await vscode.commands.executeCommand('vscode.executeHoverProvider',b.uri,b.positionAt(b.getText().indexOf('[ALPHA]')+2));
  assert.equal(help.length,1);assert.equal(help[0].contents[0].isTrusted,false);assert.equal(help[0].contents[0].supportHtml,false);
  assert.ok(help[0].contents[0].value.includes('\\[run\\]'),'authored title is literal');await replace(a,originalA);
  for(const text of [originalB.replace('Literal ALPHA','status: Planned'),originalB+'# status: Planned\n']) {
    await replace(b,text);const point=b.positionAt(b.getText().lastIndexOf('Planned')+2);
    assert.equal(workItems(await complete(point)).length,0);
    assert.equal((await vscode.commands.executeCommand('vscode.executeHoverProvider',b.uri,point)).length,0);
  }
  await replace(b,originalB);
  console.log('PASS work assistance: kind-specific statuses, valid quoted insertion, local IDs, role values, literal hover and body/comment/import exclusions');
  const pending = api.validate();await replace(b,originalB.replace('Planned','Wrong'));await pending;
  await api.validate();assert.ok(vscode.languages.getDiagnostics(b.uri).length);
  await replace(b,originalB);await api.validate();
  const selection=await open('work.json'), selectionText=selection.getText();
  await replace(selection,'{');assert.equal((await api.validate()).length,1,'broken work selection leaves requirements healthy');
  await replace(selection,selectionText);assert.equal((await api.validate()).length,2);
  assert.equal(fs.readFileSync(b.uri.fsPath,'utf8'),originalB,'unsaved checks never save');
  assert.equal(a.getText(),originalA);
  console.log('PASS work editor: mixed domains, unsaved fields/duplicates/dependencies/cycles, stale results and selection recovery');
  await replace(selection,JSON.stringify({format:'mundane-work-set-0.2',source:'mundane-work-yaml-0.2',files:['parent.mreq.yaml']}));
  assert.equal((await api.validate()).length,0,'overlapping domain selections are configuration errors');
  assert.ok(vscode.languages.getDiagnostics(selection.uri).some(d=>d.code==='editor-configuration'));
  await replace(selection,selectionText);assert.equal((await api.validate()).length,2);
  const unrelated=await open('unselected.mwork.yaml');
  assert.equal(await api.current(unrelated),null);assert.equal(vscode.languages.getDiagnostics(unrelated.uri).length,0);
  const client=require(require('node:path').join(vscode.extensions.getExtension('mundane-engineering.mundane-requirements').extensionPath,'src/client.js'));
  const invoke=client.invoke;const counts={work:0,requirements:0};
  client.invoke=(requestPath,request,...rest)=>{counts[request.source==='mundane-work-yaml-0.2'?'work':'requirements']++;return invoke(requestPath,request,...rest);};
  await api.validate();counts.work=counts.requirements=0;
  await replace(b,originalB.replace('Beta task','Unsaved title'));
  await new Promise(resolve=>setTimeout(resolve,500));
  await api.current(b);assert.deepEqual(counts,{work:1,requirements:0},'work edits preserve requirement cache');
  client.invoke=invoke;await replace(b,originalB);
  await settings.update('workProject','roadmap/work-items.json',vscode.ConfigurationTarget.WorkspaceFolder);
  const backlog=JSON.parse(fs.readFileSync(require('node:path').join(folder.uri.fsPath,'roadmap/work-items.json'),'utf8'));
  const states=await api.validate();const work=states.find(x=>x.snapshot.source==='mundane-work-yaml-0.2');
  assert.equal(work.result.valid,true);assert.equal(work.result.definitions.length,backlog.files.length);
  const card=await open(backlog.files[0]);assert.equal(card.languageId,'yaml','ordinary .yaml backlog files use the YAML provider');
  const status=card.positionAt(card.getText().indexOf('status: ')+8);
  const suggestions=await vscode.commands.executeCommand('vscode.executeCompletionItemProvider',card.uri,status);
  assert.equal(workItems(suggestions).length,6);
  assert.equal(vscode.languages.getDiagnostics(b.uri).length,0,'switching selections clears old markers');
  console.log(`PASS backlog dogfooding: ${backlog.files.length} real cards, ordinary YAML providers, overlap rejection, unselected files and independent domain cache`);
  await settings.update('workProject','',vscode.ConfigurationTarget.WorkspaceFolder);await api.validate();
}
module.exports={run};
