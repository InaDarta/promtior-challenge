function parseInline(text, keyPrefix) {
  const parts = text.split(/(\*\*[^*]+\*\*)/g)
  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return <strong key={`${keyPrefix}-${index}`}>{part.slice(2, -2)}</strong>
    }
    return part
  })
}

function parseBlocks(text) {
  const lines = text.split('\n')
  const blocks = []
  let list = null
  let paragraph = null

  for (const line of lines) {
    const listItem = line.match(/^\s*-\s+(.*)$/)
    if (listItem) {
      paragraph = null
      if (!list) {
        list = { type: 'list', items: [] }
        blocks.push(list)
      }
      list.items.push(listItem[1])
    } else if (line.trim() === '') {
      list = null
      paragraph = null
    } else {
      list = null
      if (!paragraph) {
        paragraph = { type: 'paragraph', lines: [] }
        blocks.push(paragraph)
      }
      paragraph.lines.push(line)
    }
  }

  return blocks
}

export function MarkdownMessage({ text }) {
  const blocks = parseBlocks(text)

  return blocks.map((block, blockIndex) => {
    if (block.type === 'list') {
      return (
        <ul key={blockIndex} className="markdown-message__list">
          {block.items.map((item, itemIndex) => (
            <li key={itemIndex}>{parseInline(item, `${blockIndex}-${itemIndex}`)}</li>
          ))}
        </ul>
      )
    }

    return (
      <p key={blockIndex} className="markdown-message__paragraph">
        {block.lines.map((line, lineIndex) => (
          <span key={lineIndex}>
            {lineIndex > 0 && <br />}
            {parseInline(line, `${blockIndex}-${lineIndex}`)}
          </span>
        ))}
      </p>
    )
  })
}
