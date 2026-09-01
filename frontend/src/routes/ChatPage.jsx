import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, streamChatMessage } from '../api/client'
import { AgendaPanel, Button, ErrorBanner, MarkdownMessage, Panel, Spinner, TextField } from '../components'
import { useAuth } from '../context/AuthContext.jsx'

function appendToLastAssistantMessage(messages, token) {
  const updated = [...messages]
  const last = updated[updated.length - 1]
  updated[updated.length - 1] = { ...last, text: last.text + token }
  return updated
}

function replaceLastAssistantMessage(messages, finalText) {
  const updated = [...messages]
  updated[updated.length - 1] = { ...updated[updated.length - 1], text: finalText }
  return updated
}

function dropLastMessageIfEmptyAssistant(messages) {
  const last = messages[messages.length - 1]
  if (last && last.role === 'assistant' && last.text === '') {
    return messages.slice(0, -1)
  }
  return messages
}

export function ChatPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [error, setError] = useState(null)
  const [isSending, setIsSending] = useState(false)
  const [turnCount, setTurnCount] = useState(0)

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
    setMessages((previous) => [...previous, { role: 'user', text }, { role: 'assistant', text: '' }])
    setIsSending(true)

    try {
      await streamChatMessage(text, {
        onToken: (token) => setMessages((previous) => appendToLastAssistantMessage(previous, token)),
        onDone: (finalText) => setMessages((previous) => replaceLastAssistantMessage(previous, finalText)),
      })
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        handleLogout()
        return
      }
      setError(
        err instanceof ApiError ? 'No se pudo enviar el mensaje. Probá de nuevo en unos segundos.' : err.message,
      )
      setMessages((previous) => dropLastMessageIfEmptyAssistant(previous))
    } finally {
      setIsSending(false)
      setTurnCount((previous) => previous + 1)
    }
  }

  const lastMessage = messages[messages.length - 1]
  const isWaitingForFirstToken = isSending && (!lastMessage || lastMessage.text === '')

  return (
    <div className="chat-layout">
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
            <li
              key={index}
              className={`chat__message chat__message--${message.role}${
                isSending && index === messages.length - 1 && message.role === 'assistant'
                  ? ' chat__message--streaming'
                  : ''
              }`}
            >
              {message.role === 'assistant' ? <MarkdownMessage text={message.text} /> : message.text}
            </li>
          ))}
        </ul>

        {isWaitingForFirstToken && <Spinner label="Esperando respuesta..." />}

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

      <AgendaPanel refreshKey={turnCount} />
    </div>
  )
}
