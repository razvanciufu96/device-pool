import { ref } from 'vue'

// Fake login: we only remember which user was picked. Kept in localStorage so a page
// refresh doesn't log you out.
const STORAGE_KEY = 'devicepool.user'

function load() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY))
  } catch {
    return null
  }
}

export const currentUser = ref(load())

export function login(user) {
  currentUser.value = user
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

export function logout() {
  currentUser.value = null
  localStorage.removeItem(STORAGE_KEY)
}
