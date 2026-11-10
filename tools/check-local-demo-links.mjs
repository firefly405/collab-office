import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import ts from '../collab-office-web/node_modules/typescript/lib/typescript.js'
const source = await readFile(new URL('../collab-office-web/src/utils/local-demo.ts', import.meta.url), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext } }).outputText
const { isLocalDemoUrl } = await import('data:text/javascript;base64,' + Buffer.from(compiled).toString('base64'))
const base = 'http://127.0.0.1:3000/#/dashboard'
for (const value of ['#/documents', '/api/v1/file/download/1', 'http://127.0.0.1:9000/file', 'http://localhost:3000/', 'http://[::1]:3000/', 'blob:http://127.0.0.1:3000/123']) {
  assert.equal(isLocalDemoUrl(value, base), true, value)
}
for (const value of ['https://example.com', '//example.com', 'http://localhost.example.com', 'http://127.0.0.1@evil.test', 'javascript:alert(1)', 'mailto:a@example.com', 'data:text/html,x', 'blob:https://evil.test/123']) {
  assert.equal(isLocalDemoUrl(value, base), false, value)
}
console.log('PASS demo links: 6 local URLs allowed, 8 external/unsafe URLs blocked.')
