'use strict';
const vscode = require('vscode');
const path = require('node:path');
const client = require('./client');
const { position } = require('./positions');
const { Project } = require('./project');

function activate(context) {
  const output = vscode.window.createOutputChannel('Mundane Requirements');
  const diagnostics = vscode.languages.createDiagnosticCollection('mundane-requirements');
  let disposed = false;
  const projects = new Map();
  const selector = [{ scheme: 'file', language: 'mundane-requirements' }, { scheme: 'file', language: 'yaml' }];
  function clear(project) {
    for (const uri of project.markers) diagnostics.delete(uri);
    project.markers = [];
  }
  function publish(project, state) {
    const {snapshot,result} = state;
    if (!Array.isArray(result.diagnostics) || typeof result.valid !== 'boolean') throw new Error('Invalid diagnostic response');
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
    clear(project);
    for (const [name, values] of grouped) {
      const uri = vscode.Uri.joinPath(project.folder.uri, name);
      project.markers.push(uri); diagnostics.set(uri, values);
    }
  }
  function failure(project, error) {
    clear(project); output.appendLine(error.message);
    const diagnostic = new vscode.Diagnostic(new vscode.Range(0, 0, 0, 0), error.message, vscode.DiagnosticSeverity.Error);
    diagnostic.code = 'editor-configuration'; diagnostic.source = 'Mundane';
    let uri = project.folder.uri;
    try { uri = vscode.Uri.joinPath(uri, client.relative(project.settings().get('project'))); } catch (_) { /* folder marker */ }
    project.markers.push(uri); diagnostics.set(uri, [diagnostic]);
  }
  function invalidate(project) {
    clearTimeout(project.timer); project.session.invalidate(); clear(project);
  }
  function schedule(project) {
    invalidate(project);
    if (vscode.workspace.isTrusted && project.settings().get('project')) {
      project.timer = setTimeout(() => { void project.session.get(); }, 200);
    }
  }
  function related(project, uri) {
    if (uri.scheme !== 'file') return false;
    const name = path.relative(project.folder.uri.fsPath, uri.fsPath).split(path.sep).join('/');
    return name === project.settings().get('project') || project.watched.has(name);
  }
  function changed(document) {
    for (const project of projects.values()) if (related(project, document.uri)) schedule(project);
  }
  async function validate() {
    if (!vscode.workspace.isTrusted || disposed) return [];
    const results = await Promise.all([...projects.values()].map(async project => {
      invalidate(project);
      if (!project.settings().get('project')) return null;
      const state = await project.session.get();
      return state ? {...state,folder:project.folder.uri.fsPath} : null;
    }));
    return results.filter(Boolean);
  }
  async function current(document, token, point) {
    if (token?.isCancellationRequested || !vscode.workspace.isTrusted || disposed) return null;
    const project = projects.get(vscode.workspace.getWorkspaceFolder(document.uri)?.uri.toString());
    if (!project || !project.settings().get('project')) return null;
    const version = document.version;
    const fileName = path.relative(project.folder.uri.fsPath, document.uri.fsPath).split(path.sep).join('/');
    if (!project.watched.has(fileName)) {
      await project.session.get();
      if (!project.watched.has(fileName)) return null;
    }
    const query = point ? {path:fileName,line:point.line+1,
      column:Array.from(document.lineAt(point.line).text.substring(0,point.character)).length+1} : undefined;
    const state = await project.session.get(query);
    if (!state || token?.isCancellationRequested || document.version !== version || state.generation !== project.session.generation) return null;
    const file = state.snapshot.files.find(f => f.path === fileName);
    return file ? {...state,file,folder:project.folder.uri.fsPath} : null;
  }
  function range(text, span) {
    const start = position(text, span.start.line, span.start.column);
    const end = position(text, span.end.line, span.end.column);
    return new vscode.Range(start.line, start.character, end.line, end.character);
  }
  context.subscriptions.push(vscode.languages.registerDefinitionProvider(selector, {
    async provideDefinition(document, point, token) {
      const state = await current(document, token);
      if (!state?.result.valid) return [];
      const definitions = state.result.definitions;
      const reference = definitions.flatMap(d => d.references).find(r => r.location.path === state.file.path &&
        range(state.file.text, r.location).contains(point) && !range(state.file.text, r.location).end.isEqual(point));
      if (!reference) return [];
      const targets = definitions.filter(d => d.id === reference.id);
      if (targets.length !== 1) return [];
      const target = targets[0].location;
      const text = state.snapshot.files.find(f => f.path === target.path)?.text;
      if (text === undefined) return [];
      return [new vscode.Location(vscode.Uri.file(path.join(state.folder, target.path)), range(text, target))];
    }
  }));
  context.subscriptions.push(vscode.languages.registerDocumentFormattingEditProvider(selector, {
    async provideDocumentFormattingEdits(document, options, token) {
      const state = await current(document, token);
      if (!state?.result.valid) return [];
      const formatted = state.result.formatting.find(f => f.path === state.file.path);
      if (!formatted || formatted.text === state.file.text) return [];
      return [new vscode.TextEdit(new vscode.Range(document.positionAt(0), document.positionAt(state.file.text.length)), formatted.text),
        vscode.TextEdit.setEndOfLine(vscode.EndOfLine.LF)];
    }
  }));
  context.subscriptions.push(vscode.languages.registerCompletionItemProvider(selector, {
    async provideCompletionItems(document, point, token) {
      const state = await current(document, token, point);
      if (!state) return [];
      return state.result.suggestions.map(suggestion => {
        const item = new vscode.CompletionItem(suggestion.label, vscode.CompletionItemKind.Value);
        item.insertText = suggestion.insertText; item.filterText = suggestion.insertText;
        item.detail = suggestion.detail; item.range = range(state.file.text, suggestion.location);
        return item;
      });
    }
  }, ':', ' ', '"'));
  context.subscriptions.push(vscode.languages.registerHoverProvider(selector, {
    async provideHover(document, point, token) {
      const state = await current(document, token, point);
      if (!state?.result.hover) return null;
      const info = state.result.hover;
      const definition = info.definition;
      const markdown = new vscode.MarkdownString();
      markdown.isTrusted = false; markdown.supportHtml = false;
      markdown.appendText(`${info.name} — ${definition.type}, ${definition.required ? 'required' : 'optional'}\n\n`);
      if (definition.description) markdown.appendText(definition.description + '\n\n');
      if (definition.values) markdown.appendText('Allowed values: ' + definition.values.join(', ') + '\n\n');
      markdown.appendText(`Declaration: ${info.declaration.path}:${info.declaration.start.line}`);
      return new vscode.Hover(markdown, range(state.file.text, info.location));
    }
  }));
  function synchronizeFolders() {
    const folders = vscode.workspace.workspaceFolders || [];
    for (const [key,project] of projects) if (!folders.some(f=>f.uri.toString()===key)) {
      invalidate(project); project.session.dispose(); project.watcher.dispose(); projects.delete(key);
    }
    for (const folder of folders) {
      if (projects.has(folder.uri.toString())) continue;
      const project = {folder,markers:[],watched:new Set(),settings:()=>vscode.workspace.getConfiguration('mundane',folder.uri)};
      project.session = new Project(
        generation => client.snapshot(folder.uri.fsPath,project.settings().get('project'),vscode.workspace.textDocuments,names=>{
          if(project.session.generation===generation) project.watched=new Set(names);
        }),
        (request,signal)=>client.invoke(project.settings().get('executable'),request,signal),
        state=>publish(project,state),error=>failure(project,error));
      project.watcher = vscode.workspace.createFileSystemWatcher(new vscode.RelativePattern(folder,'**/*'));
      const disk = uri=>{ if (related(project,uri)) schedule(project); };
      project.watcher.onDidChange(disk); project.watcher.onDidCreate(disk); project.watcher.onDidDelete(disk);
      projects.set(folder.uri.toString(),project); schedule(project);
    }
  }
  context.subscriptions.push(output, diagnostics,
    vscode.commands.registerCommand('mundane.validate', validate),
    vscode.workspace.onDidChangeTextDocument(event=>changed(event.document)),
    vscode.workspace.onDidOpenTextDocument(changed),
    vscode.workspace.onDidCloseTextDocument(changed),
    vscode.workspace.onDidChangeConfiguration(event=>{
      for (const project of projects.values()) if(event.affectsConfiguration('mundane',project.folder.uri)) schedule(project);
    }),
    vscode.workspace.onDidChangeWorkspaceFolders(synchronizeFolders),
    vscode.workspace.onDidGrantWorkspaceTrust(()=>{ for(const project of projects.values()) schedule(project); }),
    {dispose(){disposed=true;for(const project of projects.values()){invalidate(project);project.session.dispose();project.watcher.dispose();}}});
  synchronizeFolders();
  return {validate,current,selector};
}
module.exports = {activate};
