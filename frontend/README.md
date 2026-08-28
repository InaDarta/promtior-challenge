# frontend

SPA de la interfaz conversacional, scaffoldeada con Vite + React (`npm create vite@latest`).

## Estructura

- `src/routes/` — paginas de la app (`/login`, `/chat`)
- `src/components/` — kit de componentes compartido, sin logica de negocio
  (`Button`, `TextField`, `Panel`, `Spinner`, `ErrorBanner`)
- `src/api/client.js` — wrapper de `fetch` con `credentials: 'include'`
- `src/context/AuthContext.jsx` — esqueleto sin implementacion real (ver #39)

## Scripts

```bash
npm run dev     # levanta la SPA en modo desarrollo
npm run build   # genera dist/ para produccion
```
