import { ErrorBanner, Panel, Spinner } from '../components'

export function ChatPage() {
  return (
    <Panel title="Chat">
      <ErrorBanner message={null} />
      <Spinner label="Esperando respuesta..." />
    </Panel>
  )
}
