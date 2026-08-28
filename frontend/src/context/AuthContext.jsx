import { createContext, useContext } from 'react'

const AuthContext = createContext(null)

/**
 * Esqueleto sin implementación real: hoy no llama a `/api/me` ni persiste
 * sesión. Lo resuelve #39 (pantalla de chat) contra el endpoint real.
 */
export function AuthProvider({ children }) {
  const value = {
    user: null,
    isAuthenticated: false,
    isLoading: false,
    login: () => {
      throw new Error('AuthContext.login: no implementado todavia (ver #39)')
    },
    logout: () => {
      throw new Error('AuthContext.logout: no implementado todavia (ver #39)')
    },
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de un AuthProvider')
  }
  return context
}
