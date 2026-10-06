<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { login } from '../session'

const router = useRouter()
const users = ref([])
const error = ref('')

onMounted(async () => {
  try {
    users.value = await api.users()
  } catch (e) {
    error.value = 'Could not reach the backend. Is it running on port 8080?'
  }
})

function pick(user) {
  login(user)
  router.push('/devices')
}
</script>

<template>
  <section class="login">
    <h1>Device Pool</h1>
    <p class="muted">No passwords in this demo. Pick who you are.</p>
    <p v-if="error" class="error">{{ error }}</p>
    <ul class="user-list">
      <li v-for="u in users" :key="u.id">
        <button class="user-card" @click="pick(u)">
          <strong>{{ u.name }}</strong>
          <span class="muted">{{ u.email }} · {{ u.role.toLowerCase() }}</span>
        </button>
      </li>
    </ul>
  </section>
</template>
