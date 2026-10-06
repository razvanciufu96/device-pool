<script setup>
import { ref } from 'vue'
import { api } from '../api'

const props = defineProps({
  device: { type: Object, required: true },
})
const emit = defineEmits(['close', 'reported'])

const description = ref('')
const error = ref('')
const saving = ref(false)

async function submit() {
  error.value = ''
  saving.value = true
  try {
    await api.reportDamage(props.device.id, description.value)
    emit('reported')
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="backdrop" @click.self="emit('close')">
    <form class="dialog" @submit.prevent="submit">
      <h2>Report damage: {{ device.name }}</h2>
      <p class="muted">
        The device will be taken out of the pool until facility management has looked at it.
      </p>
      <label>
        What's wrong?
        <textarea v-model="description" rows="4" maxlength="1000" required placeholder="e.g. cracked screen, won't charge…" />
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <button type="button" class="secondary" @click="emit('close')">Cancel</button>
        <button type="submit" class="danger" :disabled="!description.trim() || saving">
          {{ saving ? 'Sending…' : 'Report damage' }}
        </button>
      </div>
    </form>
  </div>
</template>
