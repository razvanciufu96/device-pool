<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import { formatRange } from '../format'
import { currentUser } from '../session'

// The facility manager's view: what is broken, and who had it before it was reported.
const reports = ref([])
const error = ref('')

const isFacility = computed(() => currentUser.value?.role === 'FACILITY')
const open = computed(() => reports.value.filter((r) => r.open))
const resolved = computed(() => reports.value.filter((r) => !r.open))

const dateTime = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })
const fmt = (iso) => dateTime.format(new Date(iso))

async function load() {
  error.value = ''
  try {
    reports.value = await api.damageReports()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)

async function resolve(r) {
  if (!confirm(`Mark ${r.deviceName} as repaired and put it back in the pool?`)) return
  try {
    await api.resolveDamage(r.id)
    await load()
  } catch (e) {
    error.value = e.message
  }
}
</script>

<template>
  <section>
    <h1>Damage reports</h1>
    <p v-if="!isFacility" class="muted">Only facility management can mark devices as repaired.</p>
    <p v-if="error" class="error">{{ error }}</p>

    <h2>Open</h2>
    <p v-if="!open.length" class="muted">No damaged devices. 🎉</p>
    <ul class="reports">
      <li v-for="r in open" :key="r.id">
        <div class="report-head">
          <div>
            <strong>{{ r.deviceName }}</strong> <span class="muted">{{ r.assetTag }}</span>
            <span class="badge damaged">Damaged</span>
          </div>
          <button v-if="isFacility" @click="resolve(r)">Mark repaired</button>
        </div>
        <p class="description">“{{ r.description }}”</p>
        <p class="muted small">Reported by {{ r.reportedBy }} on {{ fmt(r.reportedAt) }}</p>
        <div class="holders">
          <strong class="small">Last held by</strong>
          <ol v-if="r.lastHolders.length">
            <li v-for="h in r.lastHolders" :key="h.reservationId">
              {{ h.userName }} <span class="muted">({{ formatRange(h.start, h.end) }})</span>
            </li>
          </ol>
          <p v-else class="muted small">No reservations recorded before the report.</p>
        </div>
      </li>
    </ul>

    <template v-if="resolved.length">
      <h2>Resolved</h2>
      <ul class="reports past">
        <li v-for="r in resolved" :key="r.id">
          <strong>{{ r.deviceName }}</strong>: {{ r.description }}
          <div class="muted small">
            Reported by {{ r.reportedBy }} on {{ fmt(r.reportedAt) }} · resolved by {{ r.resolvedBy }} on {{ fmt(r.resolvedAt) }}
          </div>
        </li>
      </ul>
    </template>
  </section>
</template>
