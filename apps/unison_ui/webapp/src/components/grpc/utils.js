import PropTypes from 'prop-types'
import React, { Component } from 'react'

export const isMap = schema =>
  schema.options &&
  schema.options.map_entry === 'true'

export const isDeprecated = field =>
  field.options &&
  field.options.deprecated === 'true'

const getValue = (field, schema) => {
  switch (field.type) {
    case 'STRING':
      return 'STRING'
    case 'BYTES':
      return btoa('BYTES')
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
}

export const messageExample = (schema, method) => {
  const result = {}
  const message = schema.messages.find(message => message.name === method)
  const fields = message.fields
  const oneOf = message.oneOf
  for (const field of fields) {
    let value =
      field.default !== undefined ? this.default : getValue(field, schema)
    if (field.label === 'repeated') {
      const subSchema = schema.messages.find(
        message => message.name === field.schema
      )
      if (isMap(subSchema)) {
        value = {}
        const key = getValue(subSchema.fields.find(f => f.name === 'key'))
        const val = getValue(subSchema.fields.find(f => f.name === 'value'))
        value[key] = val
      } else value = [value]
    }
    result[field.name] = value
  }
  for (const [name, fields] of Object.entries(oneOf)) {
    const field = fields[0]
    result[name] = { type: field.name, value: getValue(field, schema) }
  }
  return result
}

export const stringify = object => {
  if (typeof object === 'string') return object
  return JSON.stringify(object, null, 2)
}

function xclass (...args) {
  return args
    .filter(a => !!a)
    .join(' ')
    .trim()
}

const DEVICES = {
  mobile: '',
  tablet: '-tablet',
  desktop: '-desktop',
  large: '-hd'
}

export const Link = ({ text }) => (
  <a className="normal-case nostyle" onClick={e => e.preventDefault()}>
    <span>{text}</span>
  </a>
)
export class Col extends React.Component {
  render () {
    const {
      hide,
      keepContents,
      /* eslint-disable no-unused-vars */
      mobile,
      tablet,
      desktop,
      large,
      /* eslint-enable no-unused-vars */
      ...rest
    } = this.props

    if (hide && !keepContents) return <span />

    const classesAr = []

    for (const device in DEVICES) {
      /* eslint-disable no-prototype-builtins */
      if (!DEVICES.hasOwnProperty(device)) {
        continue
      }
      /* eslint-enable no-prototype-builtins */
      const deviceClass = DEVICES[device]
      if (device in this.props) {
        const val = this.props[device]

        if (val < 1) {
          classesAr.push('none' + deviceClass)
          continue
        }

        classesAr.push('block' + deviceClass)
        classesAr.push('col-' + val + deviceClass)
      }
    }

    if (hide) {
      classesAr.push('hidden')
    }

    const classes = xclass(rest.className, ...classesAr)

    return <section {...rest} className={classes} />
  }
}

Col.propTypes = {
  hide: PropTypes.bool,
  keepContents: PropTypes.bool,
  mobile: PropTypes.number,
  tablet: PropTypes.number,
  desktop: PropTypes.number,
  large: PropTypes.number,
  className: PropTypes.string
}

export class Row extends React.Component {
  render () {
    return (
      <div
        {...this.props}
        className={xclass(this.props.className, 'wrapper')}
      />
    )
  }
}

Row.propTypes = {
  className: PropTypes.string
}

export class Button extends React.Component {
  render () {
    return (
      <button
        {...this.props}
        className={xclass(this.props.className, 'button')}
      />
    )
  }
}
Button.propTypes = {
  className: PropTypes.string
}
Button.defaultProps = {
  className: ''
}

export class Collapse extends Component {
  render () {
    let { isOpened, children } = this.props
    children = isOpened ? children : null
    return <NoMargin>{children}</NoMargin>
  }
}

const NoMargin = ({ children }) => <div className="no-margin"> {children} </div>

NoMargin.propTypes = {
  children: PropTypes.node
}

Collapse.propTypes = {
  isOpened: PropTypes.bool,
  children: PropTypes.node.isRequired
}

Collapse.defaultProps = {
  isOpened: false
}
