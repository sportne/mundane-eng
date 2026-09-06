'use strict';
const vscode = require('vscode');
const path = require('node:path');
const client = require('./client');
const { position } = require('./positions');

function activate(context) {
  const output = vscode.window.createOutputChannel('Mundane Requirements');
  const diagnostics = vscode.languages.createDiagnosticCollection('mundane-requirements');
  let generation = 0, timer, controller, disposed = false;
  let states = [];
  const watched = new Map();
  let watchers = [];
  const selector = [{ scheme: 'file', language: 'mundane-requirements' }, { scheme: 'file', language: 'yaml' }];
  function invalidate() {
    ++generation;
    clearTimeout(timer); controller?.abort();
    states = []; diagnostics.clear();
  }
  async function refresh() {
    if (!vscode.workspace.isTrusted || disposed) return [];
    const version = generation;
    const abort = new AbortController(); controller = abort;
    const results = [];
    for (const folder of vscode.workspace.workspaceFolders || []) {
      const settings = vscode.workspace.getConfiguration('mundane', folder.uri);
      const selected = settings.get('project');
      if (!selected) continue;
      try {
        const snapshot = await client.snapshot(folder.uri.fsPath, selected, vscode.workspace.textDocuments, names => watched.set(folder.uri.fsPath, new Set(names)));
        if (version !== generation) return [];
        const result = await client.invoke(settings.get('executable'), snapshot, abort.signal);
        if (version !== generation || disposed) return [];
        if (!Array.isArray(result.diagnostics) || typeof result.valid !== 'boolean') throw new Error('Invalid diagnostic response');
        const state = { folder: folder.uri.fsPath, snapshot, result, generation: version };
        results.push(state);
        const files = [...snapshot.files, ...(snapshot.schema ? [snapshot.schema] : [])];
        const grouped = new Map();
        for (const d of result.diagnostics) {
          const file = files.find(f => f.path === d.path);
          if (!file || !Number.isInteger(d.line) || !Number.isInteger(d.column) || d.line < 1 || d.column < 1) throw new Error('Invalid diagnostic source');
          const point = position(file.text, d.line, d.column);
          const start = new vscode.Position(point.line, point.character);
          const diagnostic = new vscode.Diagnostic(new vscode.Range(start, start), d.message, vscode.DiagnosticSeverity.Error);
          diagnostic.code = d.code; diagnostic.source = 'Mundane';
          if (!grouped.has(d.path)) grouped.set(d.path, []);
          grouped.get(d.path).push(diagnostic);
        }
        for (const [name, values] of grouped) diagnostics.set(vscode.Uri.file(path.join(state.folder, name)), values);
      } catch (error) {
        if (version !== generation || disposed) return [];
        output.appendLine(error.message);
        const diagnostic = new vscode.Diagnostic(new vscode.Range(0, 0, 0, 0), error.message, vscode.DiagnosticSeverity.Error);
        diagnostic.code = 'editor-configuration'; diagnostic.source = 'Mundane';
        // Do not resolve a rejected selection outside its folder, even for an error marker.
        let uri = folder.uri;
        try { uri = vscode.Uri.joinPath(folder.uri, client.relative(selected)); } catch (_) { /* folder marker */ }
        diagnostics.set(uri, [diagnostic]);
      }
    }
    if (version === generation && !disposed) states = results;
    return results;
  }
  function schedule() { invalidate(); timer = setTimeout(() => { void refresh(); }, 200); }
  async function validate() { invalidate(); return refresh(); }
  async function current(document, token) {
    if (token?.isCancellationRequested) return null;
    const version = document.version;
    const results = await validate();
    if (token?.isCancellationRequested || document.version !== version) return null;
    const state = results.find(s => s.generation === generation && s.snapshot.files.some(f => path.join(s.folder, f.path) === document.uri.fsPath));
    if (!state) return null;
    const file = state.snapshot.files.find(f => path.join(state.folder, f.path) === document.uri.fsPath);
    return { ...state, file };
  }
  context.subscriptions.push(output, diagnostics,
    vscode.commands.registerCommand('mundane.validate', validate),
    vscode.workspace.onDidChangeTextDocument(schedule),
    vscode.workspace.onDidOpenTextDocument(schedule),
    vscode.workspace.onDidCloseTextDocument(schedule),
    vscode.workspace.onDidChangeConfiguration(schedule),
    vscode.workspace.onDidChangeWorkspaceFolders(() => { installWatchers(); schedule(); }),
    vscode.workspace.onDidGrantWorkspaceTrust(schedule),
    { dispose() { disposed = true; invalidate(); watchers.forEach(w => w.dispose()); } });
  function installWatchers() {
    watchers.forEach(w => w.dispose()); watchers = [];
    for (const folder of vscode.workspace.workspaceFolders || []) {
    const watcher = vscode.workspace.createFileSystemWatcher(new vscode.RelativePattern(folder, '**/*'));
    const changed = uri => {
      const relative = path.relative(folder.uri.fsPath, uri.fsPath).split(path.sep).join('/');
      const selected = vscode.workspace.getConfiguration('mundane', folder.uri).get('project');
      if (relative === selected || watched.get(folder.uri.fsPath)?.has(relative)) schedule();
    };
    watchers.push(watcher, watcher.onDidChange(changed), watcher.onDidCreate(changed), watcher.onDidDelete(changed));
  }
    }
  installWatchers();
  schedule();
  return { validate, current, selector };
}
module.exports = { activate };
