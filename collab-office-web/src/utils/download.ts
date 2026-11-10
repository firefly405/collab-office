import request from './request'
export async function downloadFile(fileId: string, fileName: string) {
  const result:any=await request.get('/file/download/'+fileId)
  const response=await fetch(result.data.downloadUrl)
  if(!response.ok) throw new Error('文件下载失败')
  const url=URL.createObjectURL(await response.blob()), link=document.createElement('a')
  link.href=url;link.download=fileName;link.click()
  setTimeout(()=>URL.revokeObjectURL(url),10000)
}
