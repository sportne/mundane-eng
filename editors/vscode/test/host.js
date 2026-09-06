'use strict';
const assert = require('node:assert/strict');
const vscode = require('vscode');
async function run() {
  const extension = vscode.extensions.getExtension('mundane-engineering.mundane-requirements');
  assert.ok(extension, 'extension is installed in actual host');
  const api = await extension.activate();
  const result = await api.validate();
  assert.equal(result.length, 1);
  assert.equal(result[0].result.protocol, 'mundane-editor-0.1');
  const doc = await vscode.workspace.openTextDocument(vscode.Uri.joinPath(vscode.workspace.workspaceFolders[0].uri, 'parent.mreq.yaml'));
  assert.equal(doc.languageId, 'mundane-requirements');
  console.log('PASS actual VS Code Extension Host: activation, language registration, explicit project and Java bridge');
}
module.exports = { run };
