import { Panel } from './Panel.jsx'
import { Spinner } from './Spinner.jsx'
import { ErrorBanner } from './ErrorBanner.jsx'
import { useTodayAgenda } from '../hooks/useTodayAgenda.js'

export function AgendaPanel({ refreshKey }) {
  const { agenda, isLoading, error, isWeekend } = useTodayAgenda(refreshKey)

  return (
    <Panel title="Agenda de hoy">
      <ErrorBanner message={error} />
      {isWeekend && <p className="agenda__empty">La oficina abre de lunes a viernes.</p>}
      {isLoading && <Spinner label="Cargando agenda..." />}
      {!isLoading && !isWeekend && !error && (
        <ul className="agenda__rooms">
          {agenda.map((room) => (
            <li key={room.room} className="agenda__room">
              <h3 className="agenda__room-title">Sala {room.room}</h3>
              {room.occupiedSlots.length === 0 ? (
                <p className="agenda__empty">Libre todo el día</p>
              ) : (
                <ul className="agenda__slots">
                  {mergeContiguousSlots(room.occupiedSlots).map((range) => (
                    <li key={range.start}>
                      {formatTime(range.start)}–{formatTime(range.end)}
                    </li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ul>
      )}
    </Panel>
  )
}

function formatTime(isoLocalDateTime) {
  return isoLocalDateTime.slice(11, 16)
}

function addThirtyMinutes(isoLocalDateTime) {
  const [datePart, timePart] = isoLocalDateTime.split('T')
  const [hours, minutes] = timePart.split(':').map(Number)
  const totalMinutes = hours * 60 + minutes + 30
  const hh = String(Math.floor(totalMinutes / 60) % 24).padStart(2, '0')
  const mm = String(totalMinutes % 60).padStart(2, '0')
  return `${datePart}T${hh}:${mm}:00`
}

function mergeContiguousSlots(slots) {
  const sorted = [...slots].sort((a, b) => a.start.localeCompare(b.start))
  const merged = []
  for (const slot of sorted) {
    const slotEnd = addThirtyMinutes(slot.start)
    const last = merged[merged.length - 1]
    if (last && last.end === slot.start) {
      last.end = slotEnd
    } else {
      merged.push({ start: slot.start, end: slotEnd })
    }
  }
  return merged
}
