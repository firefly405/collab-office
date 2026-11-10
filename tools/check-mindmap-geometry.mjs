import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import ts from '../collab-office-web/node_modules/typescript/lib/typescript.js'
const source=await readFile(new URL('../collab-office-web/src/views/mindmap/geometry.ts',import.meta.url),'utf8')
const js=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.ESNext,target:ts.ScriptTarget.ES2022}}).outputText
const {flatten,layoutTree}=await import('data:text/javascript;base64,'+Buffer.from(js).toString('base64'))
const node=(id,children=[])=>({id,children,content:id,parentId:null,style:{}})
const root=node('root',[node('a',[node('b')]),node('c')]);root.children[0].parentId='root';root.children[0].children[0].parentId='a';root.children[1].parentId='root'
assert.equal(flatten(root).length,4)
for(const layout of ['mindmap','org','fishbone']){const g=layoutTree(root,layout);assert.equal(g.nodes.length,4);assert.ok(g.nodes.every(n=>Number.isFinite(n.x)&&n.x>=16&&n.y>=16));assert.ok(g.width>=Math.max(...g.nodes.map(n=>n.x+184)))}
root.children[0].style.collapsed=true;assert.equal(layoutTree(root,'mindmap').nodes.length,3)
assert.equal(layoutTree(root,'mindmap',new Set(['root','a','b'])).nodes.length,3)
root.style.positions={org:{x:500,y:600}};assert.equal(layoutTree(root,'org').nodes.find(n=>n.id==='root').x,500)
root.style.positions.fishbone={x:400,y:500};assert.equal(layoutTree(root,'fishbone').nodes.find(n=>n.id==='root').x,400)
console.log('Mindmap layouts, collapse, reveal and saved positions verified.')
