import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, sendChatMessage } from '../api/client'
import { Button, ErrorBanner, Panel, Spinner, TextField } from '../components'
import { useAuth } from '../context/AuthContext.jsx'

export function ChatPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [error, setError] = useState(null)
  const [isSending, setIsSending] = useState(false)

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const text = input.trim()
    if (!text || isSending) return

    setError(null)
    setInput('')
    setMessages((previous) => [...previous, { role: 'user', text }])
    setIsSending(true)

    try {
      const { reply } = await sendChatMessage(text)
      setMessages((previous) => [...previous, { role: 'assistant', text: reply }])
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        handleLogout()
        return
      }
      setError('No se pudo enviar el mensaje. Probá de nuevo en unos segundos.')
    } finally {
      setIsSending(false)
    }
  }

  return (
    <Panel title="Chat">
      <div className="chat__header">
        <span className="chat__user">
          Conectado como <strong>{user}</strong>
        </span>
        <Button variant="secondary" onClick={handleLogout}>
          Cerrar sesión
        </Button>
      </div>

      <ErrorBanner message={error} />

      <ul className="chat__history">
        {messages.map((message, index) => (
          <li key={index} className={`chat__message chat__message--${message.role}`}>
            {message.text}
          </li>
        ))}
      </ul>

      {isSending && <Spinner label="Esperando respuesta..." />}

      <form onSubmit={handleSubmit} className="chat__form">
        <TextField
          label="Mensaje"
          id="chat-message"
          name="message"
          type="text"
          value={input}
          onChange={(event) => setInput(event.target.value)}
          autoComplete="off"
          disabled={isSending}
          required
        />
        <Button type="submit" disabled={isSending || !input.trim()}>
          Enviar
        </Button>
      </form>
    </Panel>
  )
}
