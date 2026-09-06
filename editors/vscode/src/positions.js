'use strict';
// The parser owns one-based code-point coordinates; VS Code owns UTF-16 offsets.
function position(text, line, column) {
  const lines = text.split('\n');
  const row = Math.min(Math.max(0, line - 1), lines.length - 1);
  const content = lines[row].replace(/\r$/, '');
  const character = Array.from(content).slice(0, Math.max(0, column - 1)).join('').length;
  return { line: row, character };
}
module.exports = { position };
