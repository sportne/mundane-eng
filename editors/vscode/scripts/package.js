'use strict';
const { spawnSync } = require('node:child_process');
const path = require('node:path');
const metadata = require('../versions.json');
const manifest = require('../package.json');
if (manifest.version !== metadata.version) throw new Error('Editor package version differs from generated metadata');
const output = path.resolve(__dirname, '../../../build', `mundane-requirements-${metadata.version}.vsix`);
const result = spawnSync(process.execPath, [require.resolve('@vscode/vsce/vsce'), 'package', '--no-dependencies', '--out', output,
  '--baseContentUrl', 'https://github.com/sportne/mundane-eng/blob/main/editors/vscode'], {cwd:path.resolve(__dirname,'..'),stdio:'inherit'});
if (result.error) throw result.error;
process.exitCode = result.status ?? 1;
