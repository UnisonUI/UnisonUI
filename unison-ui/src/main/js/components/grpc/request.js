import React, { Component, PureComponent } from 'react'
import { messageExample, stringify } from './utils'
import { HighlightCode } from './highlight-code'

export default class Request extends Component {
  constructor (props) {
    super(props)
    this.state = {
      value: stringify(messageExample(props.schema, props.method))
    }
    this.props.onUpdate(this.state.value)
    this.onChange = this.onChange.bind(this)
  }

  onChange (value) {
    this.props.onUpdate(value)
    this.setState({ value })
  }

  render () {
    const { value } = this.state
    return this.props.isExecute ? (
      <RequestEditor value={value} onChange={this.onChange} />
    ) : (
      <HighlightCode code={value} />
    )
  }
}

class RequestEditor extends PureComponent {
  constructor (props, context) {
    super(props, context)
    this.state = {
      value: stringify(props.value)
    }
    props.onChange(props.value)
    this.onChange = this.onChange.bind(this)
    this.onDomChange = this.onDomChange.bind(this)
  }

  onChange (value) {
    this.props.onChange(stringify(value))
  }

  onDomChange (e) {
    const inputValue = e.target.value

    this.setState(
      {
        value: inputValue
      },
      () => this.onChange(inputValue)
    )
  }

  render () {
    const { value } = this.state

    return (
      <div className="body-param">
        <textarea
          className='body-param__text'
          title=''
          value={value}
          onChange={this.onDomChange}
        />
      </div>
    )
  }
}
