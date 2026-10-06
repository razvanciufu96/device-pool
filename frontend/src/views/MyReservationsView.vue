<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import { formatRange } from '../format'

const reservations = ref([])
const error = ref('')

async function load() {
  error.value = ''
  try {
    reservations.value = await api.myReservations()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)

const current = computed(() =>
  reservations.value
    .filter((r) => r.status === 'ACTIVE' || r.status === 'UPCOMING')
    .sort((a, b) => new Date(a.start) - new Date(b.start)),
)
const past = computed(() => reservations.value.filter((r) => r.status === 'FINISHED' || r.status === 'CANCELLED'))

async function cancel(r) {
  const question = r.status === 'ACTIVE'
    ? `Return ${r.deviceName} now? Your reservation will end immediately.`
    : `Cancel your reservation of ${r.deviceName}?`
  if (!confirm(question)) return
  try {
    await api.cancel(r.id)
    await load()
  } catch (e) {
    error.value = e.message
  }
}
</script>

<template>
  <section>
    <h1>My reservations</h1>
    <p v-if="error" class="error">{{ error }}</p>

    <h2>Current &amp; upcoming</h2>
    <p v-if="!current.length" class="muted">
      Nothing booked. <RouterLink to="/devices">Find a device</RouterLink>
    </p>
    <ul class="reservations">
      <li v-for="r in current" :key="r.id">
        <div>
          <strong>{{ r.deviceName }}</strong>
          <span class="badge" :class="r.status.toLowerCase()">{{ r.status === 'ACTIVE' ? 'In use now' : 'Upcoming' }}</span>
          <div class="muted">{{ formatRange(r.start, r.end) }}</div>
        </div>
        <button class="secondary" @click="cancel(r)">
          {{ r.status === 'ACTIVE' ? 'Return now' : 'Cancel' }}
        </button>
      </li>
    </ul>

    <h2 v-if="past.length">History</h2>
    <ul class="reservations past">
      <li v-for="r in past" :key="r.id">
        <div>
          <strong>{{ r.deviceName }}</strong>
          <span class="badge" :class="r.status.toLowerCase()">{{ r.status.toLowerCase() }}</span>
          <div class="muted">{{ formatRange(r.start, r.end) }}</div>
        </div>
      </li>
    </ul>
  </section>
</template>
