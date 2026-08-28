import { useCallback, useEffect, useState } from 'react'
import { getRoomSchedule } from '../api/client'

// Mismo horario y zona que rigen el dominio (BookingRange.OFFICE_ZONE / OFFICE_OPENING / OFFICE_CLOSING):
// pedir fuera de ese rango hace que el backend rechace la consulta como fuera de horario de oficina.
const OFFICE_ZONE = 'America/Montevideo'
const OFFICE_OPENING = '08:00:00'
const OFFICE_CLOSING = '20:00:00'

// Catálogo cerrado de salas (domain/Room.java): A, B, C, D, E.
const ROOMS = ['A', 'B', 'C', 'D', 'E']

function todayInOfficeZone() {
  return new Date().toLocaleDateString('en-CA', { timeZone: OFFICE_ZONE })
}

function isWeekendInOfficeZone(date) {
  const weekday = new Intl.DateTimeFormat('en-US', { timeZone: OFFICE_ZONE, weekday: 'short' }).format(date)
  return weekday === 'Sat' || weekday === 'Sun'
}

/**
 * Agenda de hoy de las cinco salas, en el horario y zona de oficina. Un fin de semana no llama al
 * backend (la consulta se rechazaría como fuera de horario de oficina): se detecta acá mismo.
 * `refreshKey` fuerza un refetch cuando cambia (ver ChatPage: se refresca al completarse un turno
 * del chat).
 */
export function useTodayAgenda(refreshKey) {
  const [agenda, setAgenda] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [isWeekend, setIsWeekend] = useState(false)

  const load = useCallback(async () => {
    const now = new Date()
    if (isWeekendInOfficeZone(now)) {
      setIsWeekend(true)
      setAgenda([])
      setIsLoading(false)
      return
    }

    setIsWeekend(false)
    setIsLoading(true)
    setError(null)
    try {
      const today = todayInOfficeZone()
      const start = `${today}T${OFFICE_OPENING}`
      const end = `${today}T${OFFICE_CLOSING}`
      const schedules = await Promise.all(ROOMS.map((room) => getRoomSchedule(room, start, end)))
      setAgenda(schedules)
    } catch {
      setError('No se pudo cargar la agenda del día.')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load, refreshKey])

  return { agenda, isLoading, error, isWeekend }
}
