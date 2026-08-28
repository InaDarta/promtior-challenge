import { Navigate, Route, Routes } from 'react-router-dom'
import { ChatPage } from './routes/ChatPage.jsx'
import { LoginPage } from './routes/LoginPage.jsx'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/chat" element={<ChatPage />} />
      <Route path="/" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

export default App
