<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '../api'
import { atHour, formatRange, roundUpToQuarter } from '../format'
import BookingDialog from '../components/BookingDialog.vue'
import ReportDamageDialog from '../components/ReportDamageDialog.vue'

// The team lead's two questions: "what's free right now" and "what's free tomorrow".
const view = ref('now')
const typeFilter = ref('ALL')
const devices = ref([])
const loading = ref(false)
const error = ref('')
const bookingDevice = ref(null)
const damageDevice = ref(null)
const flash = ref('')

const types = ['ALL', 'PHONE', 'TABLET', 'LAPTOP']

async function load() {
  loading.value = true
  error.value = ''
  try {
    devices.value = view.value === 'now'
      ? await api.devices()
      : await api.devices(atHour(1, 0), atHour(2, 0))
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(view, load)

const shown = computed(() =>
  devices.value.filter((d) => typeFilter.value === 'ALL' || d.type === typeFilter.value),
)
const freeCount = computed(() => shown.value.filter((d) => d.available).length)

const bookingStart = computed(() => (view.value === 'now' ? roundUpToQuarter(new Date()) : atHour(1, 9)))

function showFlash(message) {
  flash.value = message
  setTimeout(() => (flash.value = ''), 4000)
}

function onBooked() {
  showFlash(`Reserved ${bookingDevice.value.name}.`)
  bookingDevice.value = null
  load()
}

function onDamageReported() {
  showFlash(`Thanks, facility management has been notified about ${damageDevice.value.name}.`)
  damageDevice.value = null
  load()
}
</script>

<template>
  <section>
    <div class="toolbar">
      <div class="tabs">
        <button :class="{ active: view === 'now' }" @click="view = 'now'">Free now</button>
        <button :class="{ active: view === 'tomorrow' }" @click="view = 'tomorrow'">Tomorrow</button>
      </div>
      <div class="filters">
        <button v-for="t in types" :key="t" class="chip" :class="{ active: typeFilter === t }" @click="typeFilter = t">
          {{ t === 'ALL' ? 'All' : t.charAt(0) + t.slice(1).toLowerCase() + 's' }}
        </button>
      </div>
    </div>

    <p class="muted">
      {{ freeCount }} of {{ shown.length }} {{ view === 'now' ? 'free right now' : 'free all day tomorrow' }}
    </p>
    <p v-if="flash" class="success">{{ flash }}</p>
    <p v-if="error" class="error">{{ error }}</p>

    <table class="devices" :class="{ loading }">
      <thead>
        <tr><th>Device</th><th>Type</th><th>Status</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="d in shown" :key="d.id">
          <td>
            <strong>{{ d.name }}</strong>
            <div class="muted">{{ d.os }} · {{ d.assetTag }}</div>
          </td>
          <td>{{ d.type.toLowerCase() }}</td>
          <td>
            <span v-if="d.damaged" class="badge damaged">Damaged</span>
            <span v-else-if="d.available" class="badge free">{{ view === 'now' ? 'Free' : 'Free all day' }}</span>
            <template v-if="!d.damaged && !d.available">
              <span class="badge busy">{{ view === 'now' ? 'In use' : 'Partly booked' }}</span>
              <div v-for="b in d.bookings" :key="b.reservationId" class="muted small">
                {{ b.userName }}: {{ formatRange(b.start, b.end) }}
              </div>
            </template>
          </td>
          <td class="right">
            <div class="row-actions">
              <button :disabled="d.damaged" :title="d.damaged ? 'Damaged devices can\'t be booked' : ''" @click="bookingDevice = d">
                Reserve
              </button>
              <button v-if="!d.damaged" class="link small" @click="damageDevice = d">Report damage</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <BookingDialog
      v-if="bookingDevice"
      :device="bookingDevice"
      :initial-start="bookingStart"
      @close="bookingDevice = null"
      @booked="onBooked"
    />
    <ReportDamageDialog
      v-if="damageDevice"
      :device="damageDevice"
      @close="damageDevice = null"
      @reported="onDamageReported"
    />
  </section>
</template>
