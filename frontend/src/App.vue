<script setup>
import { useRouter } from 'vue-router'
import { currentUser, logout } from './session'

const router = useRouter()

function switchUser() {
  logout()
  router.push('/login')
}
</script>

<template>
  <header v-if="currentUser" class="topbar">
    <strong class="brand">Device Pool</strong>
    <nav>
      <RouterLink to="/devices">Devices</RouterLink>
      <RouterLink to="/my">My reservations</RouterLink>
      <RouterLink to="/damage">Damage reports</RouterLink>
    </nav>
    <div class="who">
      <span>{{ currentUser.name }} <small class="muted">({{ currentUser.role.toLowerCase() }})</small></span>
      <button class="link" @click="switchUser">Switch user</button>
    </div>
  </header>
  <main>
    <RouterView />
  </main>
</template>
