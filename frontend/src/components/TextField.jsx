export function TextField({ label, id, ...props }) {
  return (
    <div className="text-field">
      {label && <label htmlFor={id}>{label}</label>}
      <input id={id} {...props} />
    </div>
  )
}
