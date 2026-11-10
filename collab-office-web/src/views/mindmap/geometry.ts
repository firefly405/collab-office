export type MapNode={id:string;content:string;parentId:string|null;children:MapNode[];style:Record<string,any>;styleJson:string;icon:string;note:string;link:string;sortOrder:number}
export type PlacedNode=MapNode&{x:number;y:number;depth:number}
export function flatten(root?:MapNode):MapNode[]{return root?[root,...root.children.flatMap(flatten)]:[]}
export function layoutTree(root:MapNode|undefined,layout:string,reveal?:Set<string>){
  const nodes:PlacedNode[]=[];let leaf=0,maxDepth=0
  function visit(node:MapNode,depth:number):number{
    const children=(node.style.collapsed&&!reveal?[]:node.children).filter(n=>!reveal||reveal.has(n.id)).map(n=>visit(n,depth+1))
    const center=children.length?(children[0]!+children[children.length-1]!)/2:leaf++*104+40
    const position=node.style.positions?.[layout],x=layout==='org'?center*2:32+depth*240,y=layout==='org'?32+depth*140:center
    nodes.push({...node,x:Number.isFinite(position?.x)?Math.max(16,Math.min(20000,position.x)):x,y:Number.isFinite(position?.y)?Math.max(16,Math.min(20000,position.y)):y,depth});maxDepth=Math.max(maxDepth,depth);return center
  }
  if(root)visit(root,0)
  if(layout==='fishbone'&&root){const rootNode=nodes.find(n=>n.id===root.id)!;if(!root.style.positions?.fishbone){rootNode.x=32;rootNode.y=230}const branches=root.children.filter(n=>nodes.some(p=>p.id===n.id));branches.forEach((branch,i)=>{const placed=nodes.find(n=>n.id===branch.id)!,x=280+i*260,y=i%2?390:70,offsetX=x-placed.x,offsetY=y-placed.y;const ids=new Set(flatten(branch).map(n=>n.id));for(const n of nodes)if(ids.has(n.id)&&!n.style.positions?.fishbone){n.x+=offsetX;n.y=Math.max(16,n.y+offsetY)}})}
  return {nodes,width:Math.max(700,...nodes.map(n=>n.x+220)),height:Math.max(450,...nodes.map(n=>n.y+110)),maxDepth}
}
