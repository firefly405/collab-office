export type BoardOperation={id:string;tool:string;color:string;width:number;points?:number[][];x:number;y:number;w:number;h:number;text?:string;elementId?:number;actorId?:string;active?:boolean;createdAt?:string}
export function drawOperation(c:CanvasRenderingContext2D,op:BoardOperation){
 if(op.tool==='clear'){c.clearRect(0,0,1920,1080);c.fillStyle='#ffffff';c.fillRect(0,0,1920,1080);return}
 c.save();c.strokeStyle=op.tool==='eraser'?'#ffffff':op.color;c.fillStyle=c.strokeStyle;c.lineWidth=op.tool==='eraser'?op.width*3:op.width;c.lineCap='round';c.lineJoin='round';c.beginPath()
 const x=op.x||0,y=op.y||0,w=op.w??x,h=op.h??y
 switch(op.tool){
 case 'pen':case 'eraser':{const points=op.points||[];if(points.length){c.moveTo(points[0]![0]!,points[0]![1]!);for(const p of points.slice(1))c.lineTo(p[0]!,p[1]!);if(points.length===1)c.lineTo(points[0]![0]!+.1,points[0]![1]!);c.stroke()}break}
 case 'rect':c.strokeRect(x,y,w-x,h-y);break
 case 'circle':c.ellipse((x+w)/2,(y+h)/2,Math.abs(w-x)/2,Math.abs(h-y)/2,0,0,Math.PI*2);c.stroke();break
 case 'triangle':c.moveTo((x+w)/2,y);c.lineTo(w,h);c.lineTo(x,h);c.closePath();c.stroke();break
 case 'line':case 'arrow':{c.moveTo(x,y);c.lineTo(w,h);if(op.tool==='arrow'){const angle=Math.atan2(h-y,w-x),size=Math.max(12,op.width*4);c.moveTo(w-size*Math.cos(angle-.5),h-size*Math.sin(angle-.5));c.lineTo(w,h);c.lineTo(w-size*Math.cos(angle+.5),h-size*Math.sin(angle+.5))}c.stroke();break}
 case 'text':c.font=`${Math.max(14,op.width*5)}px Arial`;for(const [i,line] of (op.text||'').split('\n').entries())c.fillText(line,x,y+i*Math.max(18,op.width*6));break
 }
 c.restore()
}
