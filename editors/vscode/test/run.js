'use strict';
const path = require('node:path');
const fs = require('node:fs/promises');
const { runTests } = require('@vscode/test-electron');
async function main() {
  const root = path.resolve(__dirname, '../../..');
  const workspace = path.join(root, 'build/editor-test-workspace');
  await fs.rm(workspace, { recursive: true, force: true });
  await fs.mkdir(path.join(workspace, '.vscode'), { recursive: true });
  const fixtures = path.join(__dirname, 'fixtures');
  await fs.cp(fixtures, workspace, { recursive: true });
  await fs.cp(path.join(root,'roadmap'),path.join(workspace,'roadmap'),{recursive:true});
  await require('./stage-imports').stage(root,workspace);
  await require('./stage-engineering').stage(root,workspace);
  await fs.writeFile(path.join(workspace, '.vscode/settings.json'), JSON.stringify({
    'mundane.project': 'editor.json',
    'mundane.engineeringExecutable': path.join(root,'build/maintained/mundane-engineering-editor'),
    'mundane.executable': path.join(root, 'build/maintained/mundane-editor'),
    'security.workspace.trust.enabled': false
  }));
  await runTests({
    version: '1.109.5',
    extensionDevelopmentPath: path.resolve(__dirname, '..'),
    extensionTestsPath: path.join(__dirname, 'host.js'),
    cachePath: path.join(root, 'build/vscode-test'),
    launchArgs: [workspace, '--disable-workspace-trust', '--no-sandbox', '--disable-gpu', '--skip-welcome', '--skip-release-notes'],
    extensionTestsEnv: { MUNDANE_TEST_ROOT: workspace }
  });
}
main().catch(error => { console.error(error); process.exitCode = 1; });
