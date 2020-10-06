import { Light as SyntaxHighlighter } from 'react-syntax-highlighter'
import json from 'react-syntax-highlighter/dist/esm/languages/hljs/json'
import obsidian from 'react-syntax-highlighter/dist/esm/styles/hljs/obsidian'
import React, { Component } from 'react'

SyntaxHighlighter.registerLanguage('json', json)

export default const HighlightCode extends Component {
  render () {
    let { className, code } = this.props
    if (typeof code !== 'string') {
      code = JSON.stringify(code, null, 2)
    }
    return (
      <div className="highlight-code">
        <SyntaxHighlighter
          className={className + ' microlight'}
          style={obsidian}
        >
          {code}
        </SyntaxHighlighter>
      </div>
    )
  }
}
