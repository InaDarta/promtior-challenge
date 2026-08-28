export function Button({ children, type = 'button', variant = 'primary', ...props }) {
  return (
    <button type={type} className={`button button--${variant}`} {...props}>
      {children}
    </button>
  )
}
