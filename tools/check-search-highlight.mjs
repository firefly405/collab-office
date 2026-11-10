import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import ts from '../collab-office-web/node_modules/typescript/lib/typescript.js'
const source=await readFile(new URL('../collab-office-web/src/views/search/highlight.ts',import.meta.url),'utf8')
const {highlight}=await import('data:text/javascript;base64,'+Buffer.from(ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.ESNext}}).outputText).toString('base64'))
assert.deepEqual(highlight('Plan plan','plan'),[{text:'Plan',match:true},{text:' ',match:false},{text:'plan',match:true}])
assert.equal(highlight('<img onerror=alert(1)>','img').map(p=>p.text).join(''),'<img onerror=alert(1)>')
assert.equal(highlight('a[bc]','[')[1].match,true)
assert.deepEqual(highlight('文本','不存在'),[{text:'文本',match:false}])
console.log('Literal safe-text highlights verified.')
