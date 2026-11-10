import request from '@/utils/request'

export function globalSearch(keyword: string, scope?: string) {
  return request.get('/search/global', { params: { keyword, scope } })
}

export function getSearchSuggest(keyword: string) {
  return request.get('/search/suggest', { params: { keyword } })
}
