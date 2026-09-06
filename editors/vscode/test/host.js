'use strict';
const assert = require('node:assert/strict');
const vscode = require('vscode');
async function run() {
  const extension = vscode.extensions.getExtension('mundane-engineering.mundane-requirements');
  assert.ok(extension, 'extension is installed in actual host');
  const api = await extension.activate();
  await new Promise(resolve => setTimeout(resolve, 350));
  const result = await api.validate();
  assert.equal(result.length, 1);
  assert.equal(result[0].result.protocol, 'mundane-editor-0.1');
  const doc = await vscode.workspace.openTextDocument(vscode.Uri.joinPath(vscode.workspace.workspaceFolders[0].uri, 'parent.mreq.yaml'));
  assert.equal(doc.languageId, 'mundane-requirements');
  assert.equal(result[0].result.valid, true);
  const child = await vscode.workspace.openTextDocument(vscode.Uri.joinPath(vscode.workspace.workspaceFolders[0].uri, 'child.mreq.yaml'));
  const original = child.getText();
  async function replace(document, text) {
    const edit = new vscode.WorkspaceEdit();
    edit.replace(document.uri, new vscode.Range(document.positionAt(0), document.positionAt(document.getText().length)), text);
    assert.ok(await vscode.workspace.applyEdit(edit));
  }
  await replace(child, original.replace('PARENT', 'MISSING'));
  await api.validate();
  assert.ok(vscode.languages.getDiagnostics(child.uri).some(d => d.code === 'dangling-reference'));
  const first = api.validate();
  await replace(child, original);
  await first;
  await api.validate();
  assert.equal(vscode.languages.getDiagnostics(child.uri).length, 0, 'new buffer clears errors and supersedes stale request');
  assert.equal(require('node:fs').readFileSync(child.uri.fsPath, 'utf8'), original, 'validation never saves the buffer');
  await replace(child, 'format: [\n');
  await api.validate();
  assert.ok(vscode.languages.getDiagnostics(child.uri).length);
  await replace(child, original); await api.validate();
  const selection = await vscode.workspace.openTextDocument(vscode.Uri.joinPath(vscode.workspace.workspaceFolders[0].uri, 'editor.json'));
  const selectionText = selection.getText();
  const schema = await vscode.workspace.openTextDocument(vscode.Uri.joinPath(vscode.workspace.workspaceFolders[0].uri, 'schema.json'));
  const schemaText = schema.getText();
  const parentText = doc.getText();
  const withAttributes = text => text.replace('mundanereq-yaml-0.3', 'mundanereq-yaml-0.4')
    .replace('requirements:', 'attributeSchema: "logger-metadata"\nrequirements:') + '    attributes:\n      discipline: "software"\n';
  await replace(selection, JSON.stringify({format:'mundane-editor-project-0.1', source:'yaml-0.4', files:['parent.mreq.yaml','child.mreq.yaml'], attributeSchema:'schema.json'}));
  await replace(doc, withAttributes(parentText)); await replace(child, withAttributes(original));
  assert.equal((await api.validate())[0].result.valid, true);
  await replace(schema, schemaText.replace('"software"', '"firmware"'));
  await api.validate();
  assert.ok(vscode.languages.getDiagnostics(child.uri).some(d => d.code === 'attribute-value'));
  await replace(schema, '{\n'); await api.validate();
  assert.ok(vscode.languages.getDiagnostics(schema.uri).some(d => d.code === 'attribute-schema-invalid'));
  await replace(schema, schemaText); await replace(selection, selectionText);
  await replace(doc, parentText); await replace(child, original); await api.validate();
  assert.equal(vscode.languages.getDiagnostics(schema.uri).length, 0);
  console.log('PASS unsaved declaration changes, enum diagnostics and schema repair');
  console.log('PASS unsaved syntax/reference diagnostics, repair, stale request rejection and unchanged disk');
  console.log('PASS actual VS Code Extension Host: activation, language registration, explicit project and Java bridge');
}
module.exports = { run };
