export function highlight(value:unknown,term:string):{text:string;match:boolean}[]{
  const text=String(value??''),query=term.trim().toLocaleLowerCase()
  if(!query)return [{text,match:false}]
  const lower=text.toLocaleLowerCase(),parts:{text:string;match:boolean}[]=[];let cursor=0,index=lower.indexOf(query)
  while(index>=0){if(index>cursor)parts.push({text:text.slice(cursor,index),match:false});parts.push({text:text.slice(index,index+query.length),match:true});cursor=index+query.length;index=lower.indexOf(query,cursor)}
  if(cursor<text.length)parts.push({text:text.slice(cursor),match:false})
  return parts
}
