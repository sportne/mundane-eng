'use strict';
const vscode = require('vscode');
const path = require('node:path');
const client = require('./client');

function activate(context) {
  const output = vscode.window.createOutputChannel('Mundane Requirements');
  context.subscriptions.push(output);
  async function validate() {
    if (!vscode.workspace.isTrusted) throw new Error('Trust the workspace before running the compiler');
    const results = [];
    for (const folder of vscode.workspace.workspaceFolders || []) {
      const settings = vscode.workspace.getConfiguration('mundane', folder.uri);
      const selected = settings.get('project');
      if (!selected) continue;
      const request = await client.snapshot(folder.uri.fsPath, selected, vscode.workspace.textDocuments);
      const result = await client.invoke(settings.get('executable'), request);
      results.push({ folder: folder.uri.fsPath, result });
    }
    return results;
  }
  context.subscriptions.push(vscode.commands.registerCommand('mundane.validate', async () => {
    try { const results = await validate(); output.appendLine(`Checked ${results.length} selected projects`); return results; }
    catch (error) { output.appendLine(error.message); vscode.window.showErrorMessage(`Mundane: ${error.message}`); return []; }
  }));
  return { validate };
}
module.exports = { activate };
