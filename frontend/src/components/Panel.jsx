export function Panel({ title, children }) {
  return (
    <section className="panel">
      {title && <h2 className="panel__title">{title}</h2>}
      <div className="panel__body">{children}</div>
    </section>
  )
}
