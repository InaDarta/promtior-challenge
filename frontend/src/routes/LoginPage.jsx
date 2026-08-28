import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { Button, ErrorBanner, Panel, TextField } from '../components'
import { useAuth } from '../context/AuthContext.jsx'

const DEMO_CREDENTIALS = [
  { username: 'User1', password: 'TechnicalChallengePromtior' },
  { username: 'User2', password: 'TechnicalChallengePromtior' },
]

export function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await login(username, password)
      navigate('/chat', { replace: true })
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 401
          ? 'Usuario o contraseña incorrectos.'
          : 'No se pudo iniciar sesión. Probá de nuevo en unos segundos.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="login-page">
      <Panel title="Iniciar sesión">
        <form onSubmit={handleSubmit}>
          <ErrorBanner message={error} />
          <TextField
            label="Usuario"
            id="username"
            name="username"
            type="text"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            required
          />
          <TextField
            label="Contraseña"
            id="password"
            name="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
          />
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Entrando...' : 'Entrar'}
          </Button>
        </form>
        <p className="login-hint__title">Credenciales de la demo:</p>
        <ul className="login-hint__list">
          {DEMO_CREDENTIALS.map((credential) => (
            <li key={credential.username}>
              <code>{credential.username}</code> / <code>{credential.password}</code>
            </li>
          ))}
        </ul>
      </Panel>
    </div>
  )
}
