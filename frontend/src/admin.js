import { reactive } from 'vue'

export const adminStore = reactive({
  key: localStorage.getItem('adminKey') || '',
})

export function setAdminKey(key) {
  adminStore.key = key
  localStorage.setItem('adminKey', key)
}

export function authHeaders() {
  return {
    'Content-Type': 'application/json',
    'X-Admin-Key': adminStore.key,
  }
}
