import request from '@/utils/request'

export function uploadFileMeta(fileName: string, fileSize: number, folderId?: number) {
  return request.post('/file/upload', null, {
    params: {
      fileName,
      fileSize,
      folderId,
    },
  })
}
