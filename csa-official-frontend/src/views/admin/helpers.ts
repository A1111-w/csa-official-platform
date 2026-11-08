export function errorText(error: unknown) {
  return error instanceof Error ? error.message : '请求失败，请稍后重试。'
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function downloadJson(value: unknown, filename: string) {
  downloadBlob(new Blob([JSON.stringify(value, null, 2)], { type: 'application/json' }), filename)
}
