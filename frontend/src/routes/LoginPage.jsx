import { Button, Panel, TextField } from '../components'

export function LoginPage() {
  return (
    <Panel title="Iniciar sesion">
      <form>
        <TextField label="Usuario" id="username" name="username" type="text" />
        <TextField label="Contrasena" id="password" name="password" type="password" />
        <Button type="submit">Entrar</Button>
      </form>
    </Panel>
  )
}
