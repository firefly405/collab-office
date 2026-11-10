import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import ts from '../collab-office-web/node_modules/typescript/lib/typescript.js'

const source = await readFile(new URL('../collab-office-web/src/utils/token.ts', import.meta.url), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext } }).outputText
const { readRefreshResponse } = await import('data:text/javascript;base64,' + Buffer.from(compiled).toString('base64'))

assert.deepEqual(readRefreshResponse({ code: 200, data: 'new-access' }, 'old-refresh'), {
  accessToken: 'new-access', refreshToken: 'old-refresh',
})
assert.deepEqual(readRefreshResponse({ code: 200, data: { accessToken: 'new-access', refreshToken: 'new-refresh' } }, 'old'), {
  accessToken: 'new-access', refreshToken: 'new-refresh',
})
for (const response of [undefined, null, {}, { code: 401 }, { code: 401, data: 'bad' },
  { code: 200 }, { code: 200, data: null }, { code: 200, data: '' },
  { code: 200, data: { accessToken: ' ' } }, { code: 200, data: { accessToken: 123 } }]) {
  assert.throws(() => readRefreshResponse(response, 'old'), /登录已过期/)
}
console.log('PASS refresh response: string/pair success and 10 invalid/expired payloads rejected.')

if (process.argv.includes('--live')) {
  const { build } = await import('../collab-office-web/node_modules/esbuild/lib/main.js')
  const { createRequire } = await import('node:module')
  const { runInNewContext } = await import('node:vm')
  const require = createRequire(new URL('../collab-office-web/package.json', import.meta.url))
  const axios = require('axios')
  axios.defaults.baseURL = 'http://127.0.0.1:3000'
  let refreshCalls = 0
  const observer = axios.interceptors.request.use(config => {
    if (config.url === '/api/v1/auth/refresh-token') refreshCalls++
    return config
  })
  const compiled = await build({
    entryPoints: [new URL('../collab-office-web/src/utils/request.ts', import.meta.url).pathname.replace(/^\/(\w:)/, '$1')],
    bundle: true, platform: 'node', format: 'cjs', packages: 'external', write: false,
    plugins: [{ name: 'toast-capture', setup(build) {
      build.onResolve({ filter: /^element-plus$/ }, () => ({ path: 'toast', namespace: 'test-ui' }))
      build.onLoad({ filter: /.*/, namespace: 'test-ui' }, () => ({
        contents: 'export const ElMessage = { error(message) { globalThis.messages.push(message) } }', loader: 'js',
      }))
    } }],
  })
  // Only browser storage/navigation/toasts are emulated; Axios and backend HTTP are real.
  const storage = new Map([['access_token', 'invalid-runtime-test-access'], ['refresh_token', 'invalid-runtime-test-refresh']])
  const module = { exports: {} }
  const context = { module, exports: module.exports, require, messages: [], window: { location: { hash: '#/settings' } },
    localStorage: { getItem: key => storage.get(key) ?? null, setItem: (key, value) => storage.set(key, value), removeItem: key => storage.delete(key) } }
  runInNewContext(compiled.outputFiles[0].text, context)
  const request = module.exports.default
  request.defaults.baseURL = 'http://127.0.0.1:3000/api/v1'
  try {
    const results = await Promise.allSettled([request.get('/user/info'), request.get('/space/my')])
    assert.ok(results.every(result => result.status === 'rejected' && /登录已过期/.test(result.reason.message)))
    assert.equal(refreshCalls, 1)
    assert.equal(context.window.location.hash, '#/login')
    assert.equal(storage.size, 0)
    assert.equal(context.messages.length, 1)
    console.log('PASS live backend: concurrent expired sessions share one refresh, reject cleanly and return to login.')
  } finally {
    axios.interceptors.request.eject(observer)
  }
}
