import React, { Component, PureComponent } from 'react'
import { Collapse } from './utils'
import { ChevronDown, ChevronRight } from 'react-feather'
import HighlightCode from './highlight-code'
import cx from 'classnames'

const stringify = object => {
  if (typeof object === 'string') return object
  return JSON.stringify(object, null, 2)
}

const messageExample = (schema, method) => {
  const result = {}
  const fields = schema.messages.find(message => message.name === method).fields
  for (const field of fields) {
    let value =
      field.default !== undefined
        ? this.default
        : (type => {
          switch (type) {
            case 'STRING':
              return 'STRING'
            case 'BOOL':
              return true
            case 'MESSAGE':
              return messageExample(schema, field.schema)
            case 'ENUM':
              return schema.enums.find(e => e.name === field.schema).values[0]
            case 'FLOAT':
            case 'DOUBLE':
              return 0.5
            default:
              return 42
          }
        })(field.type)
    if (field.label === 'repeated') {
      value = [value]
    }
    result[field.name] = value
  }
  return result
}

const Link = ({ text }) => (
  <a className="nostyle normal-case" onClick={e => e.preventDefault()}>
    <span>{text}</span>
  </a>
)

const TryItOut = ({ onTryoutClick, onCancelClick, enabled }) => (
  <div className="try-out">
    {enabled ? (
      <button className="btn try-out__btn cancel" onClick={onCancelClick}>
        Cancel
      </button>
    ) : (
      <button className="btn try-out__btn" onClick={onTryoutClick}>
        Try it out
      </button>
    )}
  </div>
)

export default class Services extends Component {
  render () {
    const { server, schema } = this.props
    const services = schema.services.map(service => (
      <Service
        key={`service-${service.fullName}`}
        service={service}
        schema={schema}
        server={server}
      />
    ))
    return <div>{services}</div>
  }
}

class Service extends Component {
  constructor (props) {
    super(props)
    this.state = { isVisible: true }
    this.toggleVisibility = this.toggleVisibility.bind(this)
  }

  toggleVisibility () {
    this.setState({ isVisible: !this.state.isVisible })
  }

  render () {
    const { isVisible } = this.state
    const { server, service, schema } = this.props
    return (
      <div
        className={
          isVisible ? 'opblock-tag-section is-open' : 'opblock-tag-section'
        }
      >
        <h4
          onClick={this.toggleVisibility}
          className="opblock-tag no-desc"
          data-is-open={isVisible}
        >
          <Link text={service.fullName} />
          <small></small>
          <button
            className="expand-operation"
            title={isVisible ? 'Collapse service' : 'Expand service'}
            onClick={this.toggleVisibility}
          >
            {isVisible ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
          </button>
        </h4>
        <Collapse isOpened={isVisible}>
          {service.methods.map(method => (
            <Method
              key={`${service.fullName}.${method.name}`}
              server={server}
              schema={schema}
              method={method}
            />
          ))}
        </Collapse>
      </div>
    )
  }
}

class Method extends Component {
  constructor (props) {
    super(props)
    this.state = {
      isVisible: false,
      tryItOutEnabled: false,
      executeInProgress: false,
      response: null
    }
    this.toggleVisibility = this.toggleVisibility.bind(this)
    this.onTryoutClick = this.onTryoutClick.bind(this)
    this.onCancelClick = this.onCancelClick.bind(this)
    this.onExecute = this.onExecute.bind(this)
    this.onClear = this.onClear.bind(this)
  }

  onClear () {
    this.setState({ executeInProgress: false })
  }

  onExecute () {
    this.setState({ executeInProgress: true })
  }

  toggleVisibility () {
    this.setState({ isVisible: !this.state.isVisible })
  }

  onTryoutClick () {
    this.setState({ tryItOutEnabled: true })
  }

  onCancelClick () {
    this.setState({ tryItOutEnabled: false, executeInProgress: false })
  }

  render () {
    const methodName = 'post'
    const { method, schema } = this.props
    const {
      tryItOutEnabled,
      isVisible,
      executeInProgress,
      response
    } = this.state
    return (
      <div
        className={
          isVisible
            ? `opblock opblock-${methodName} is-open`
            : `opblock opblock-${methodName}`
        }
      >
        <div
          className={`opblock-summary opblock-summary-${methodName}`}
          onClick={this.toggleVisibility}
        >
          <span className="opblock-summary-method">RPC</span>
          <span className="opblock-summary-path">
            <Link text={method.name} />
          </span>
        </div>
        <Collapse isOpened={isVisible}>
          <div className="opblock-body">
            <div className="opblock-section opblock-section-request-body">
              <div className="opblock-section-header">
                <h4 className="opblock-title parameter__name">
                  Request (<small>{method.inputType}</small>)
                </h4>
                <TryItOut
                  enabled={tryItOutEnabled}
                  onCancelClick={this.onCancelClick}
                  onTryoutClick={this.onTryoutClick}
                />
              </div>
              <div className="opblock-description-wrapper">
                <RequestBody
                  schema={schema}
                  method={method.inputType}
                  isExecute={tryItOutEnabled}
                />
              </div>
            </div>
            <div
              className={
                !tryItOutEnabled || !response ? 'execute-wrapper' : 'btn-group'
              }
            >
              {!tryItOutEnabled ? null : <Execute onExecute={this.onExecute} />}

              {!tryItOutEnabled || !response ? null : (
                <Clear onClear={this.onClear} />
              )}
            </div>

            {executeInProgress ? (
              <div className="loading-container">
                <div className="loading"></div>
              </div>
            ) : null}
            <div className="responses-wrapper">
              <div className="opblock-section-header">
                <h4>
                  Response (<small>{method.outputType}</small>)
                </h4>
              </div>
              <div className="responses-inner">
                <HighlightCode
                  code={messageExample(schema, method.outputType)}
                />
              </div>
            </div>
          </div>
        </Collapse>
      </div>
    )
  }
}

class Execute extends Component {
  constructor (props) {
    super(props)
    this.onClick = this.onClick.bind(this)
  }

  onClick () {
    this.props.onExecute()
  }

  render () {
    return (
      <button
        className="btn execute opblock-control__btn"
        onClick={this.onClick}
      >
        Execute
      </button>
    )
  }
}
class Clear extends Component {
  constructor (props) {
    super(props)
    this.onClick = this.onClick.bind(this)
  }

  onClick () {
    this.props.onClear()
  }

  render () {
    return (
      <button
        className="btn btn-clear opblock-control__btn"
        onClick={this.onClick}
      >
        Clear
      </button>
    )
  }
}

class RequestBody extends Component {
  constructor (props) {
    super(props)
    this.state = {
      value: stringify(messageExample(props.schema, props.method))
    }
    this.onChange = this.onChange.bind(this)
  }

  onChange (value) {
    this.setState({ value })
  }

  render () {
    const { value } = this.state
    return this.props.isExecute ? (
      <RequestEditor value={value} onChange={this.onChange} errors={[]} />
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
    const { errors } = this.props
    const { value } = this.state
    const isInvalid = errors.size > 0

    return (
      <div className="body-param">
        <textarea
          className={cx('body-param__text', { invalid: isInvalid })}
          title={errors.size ? errors.join(', ') : ''}
          value={value}
          onChange={this.onDomChange}
        />
      </div>
    )
  }
}
