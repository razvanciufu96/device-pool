const time = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' })
const dayTime = new Intl.DateTimeFormat(undefined, {
  weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
})

function isSameDay(a, b) {
  return a.toDateString() === b.toDateString()
}

/** "Tue 6 Oct, 14:00 – 16:00", or both full dates when the range spans days. */
export function formatRange(start, end) {
  const s = new Date(start)
  const e = new Date(end)
  return isSameDay(s, e)
    ? `${dayTime.format(s)} – ${time.format(e)}`
    : `${dayTime.format(s)} – ${dayTime.format(e)}`
}

/** Value for <input type="datetime-local">, in the browser's local time. */
export function toLocalInput(date) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function roundUpToQuarter(date) {
  const d = new Date(date)
  d.setSeconds(0, 0)
  d.setMinutes(Math.ceil(d.getMinutes() / 15) * 15)
  return d
}

export function atHour(dayOffset, hour) {
  const d = new Date()
  d.setDate(d.getDate() + dayOffset)
  d.setHours(hour, 0, 0, 0)
  return d
}
