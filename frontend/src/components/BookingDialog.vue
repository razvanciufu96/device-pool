<script setup>
import { computed, ref } from 'vue'
import { api, ApiError } from '../api'
import { atHour, formatRange, roundUpToQuarter, toLocalInput } from '../format'

const props = defineProps({
  device: { type: Object, required: true },
  // Pre-fill for the form, so "Book" from the Tomorrow tab proposes tomorrow.
  initialStart: { type: Date, required: true },
})
const emit = defineEmits(['close', 'booked'])

const start = ref(toLocalInput(props.initialStart))
const end = ref(toLocalInput(new Date(props.initialStart.getTime() + 60 * 60 * 1000)))
const error = ref('')
const conflicts = ref([])
const saving = ref(false)

const presets = [
  { label: 'Next hour', range: () => { const s = roundUpToQuarter(new Date()); return [s, new Date(s.getTime() + 3600e3)] } },
  { label: 'Tomorrow morning', range: () => [atHour(1, 9), atHour(1, 12)] },
  { label: 'Tomorrow afternoon', range: () => [atHour(1, 13), atHour(1, 17)] },
]

function applyPreset(p) {
  const [s, e] = p.range()
  start.value = toLocalInput(s)
  end.value = toLocalInput(e)
}

const invalid = computed(() => !start.value || !end.value || new Date(end.value) <= new Date(start.value))

async function submit() {
  error.value = ''
  conflicts.value = []
  saving.value = true
  try {
    await api.reserve(props.device.id, new Date(start.value), new Date(end.value))
    emit('booked')
  } catch (e) {
    error.value = e.message
    if (e instanceof ApiError) conflicts.value = e.conflicts
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="backdrop" @click.self="emit('close')">
    <form class="dialog" @submit.prevent="submit">
      <h2>Reserve {{ device.name }}</h2>
      <p class="muted">{{ device.os }} · {{ device.assetTag }}</p>

      <div class="presets">
        <button v-for="p in presets" :key="p.label" type="button" class="chip" @click="applyPreset(p)">
          {{ p.label }}
        </button>
      </div>

      <label>From <input v-model="start" type="datetime-local" required /></label>
      <label>Until <input v-model="end" type="datetime-local" required /></label>

      <div v-if="error" class="error">
        {{ error }}
        <ul v-if="conflicts.length">
          <li v-for="c in conflicts" :key="c.reservationId">
            {{ c.userName }}: {{ formatRange(c.start, c.end) }}
          </li>
        </ul>
      </div>

      <div class="actions">
        <button type="button" class="secondary" @click="emit('close')">Cancel</button>
        <button type="submit" :disabled="invalid || saving">{{ saving ? 'Reserving…' : 'Reserve' }}</button>
      </div>
    </form>
  </div>
</template>
