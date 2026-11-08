import { ref } from 'vue'
export const notifications = ref<Array<{ id: number; kind: string; message: string }>>([])
let nextId = 0
function show(kind: string, message: string) {
  const id = ++nextId
  notifications.value = [...notifications.value.slice(-3), { id, kind, message }]
  setTimeout(() => dismissNotice(id), 6500)
}
export function dismissNotice(id: number) {
  notifications.value = notifications.value.filter(item => item.id !== id)
}
export const notify = {
  success: (message: string) => show('success', message),
  error: (message: string) => show('error', message),
  info: (message: string) => show('info', message),
}
