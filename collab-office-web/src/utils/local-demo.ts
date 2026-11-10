export function isLocalDemoUrl(value: string, base: string): boolean {
  try {
    const url = new URL(value, base)
    const host = url.protocol === 'blob:' ? new URL(url.origin).hostname : url.hostname
    return ['http:', 'https:', 'blob:'].includes(url.protocol) && ['127.0.0.1', 'localhost', '[::1]'].includes(host)
  } catch {
    return false
  }
}
