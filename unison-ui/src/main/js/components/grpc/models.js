import React, { Component } from 'react'
import { isDeprecated, isMap, Collapse } from './utils'
import { ChevronDown, ChevronRight } from 'react-feather'

const braceOpen = '{'
const braceClose = '}'

const bracketOpen = '['
const bracketClose = ']'

export default class Models extends Component {
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
    return (
      <section className={isVisible ? 'models is-open' : 'models'}>
        <h4 onClick={this.toggleVisibility}>
          <span>Messages/Enums</span>
          {isVisible ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
        </h4>
        <Collapse isOpened={isVisible}>
          {this.props.spec.messages
            .filter(schema => !isMap(schema))
            .map(schema => (
              <Model
                key={`models-section-${schema.name}`}
                schema={schema}
                spec={this.props.spec}
              />
            ))}
          {this.props.spec.enums.map(value => (
            <Model key={`models-section-${value.name}`} schema={value} />
          ))}
        </Collapse>
      </section>
    )
  }
}

class Model extends Component {
  constructor (props) {
    super(props)
    this.state = { isCollapsed: true }
    this.toggleCollapsed = this.toggleCollapsed.bind(this)
  }

  toggleCollapsed () {
    this.setState({ isCollapsed: !this.state.isCollapsed })
  }

  render () {
    const { isCollapsed } = this.state
    const { fields, name, oneOf, values } = this.props.schema
    return (
      <div id={`model-${name}`} className="model-container" data-name={name}>
        <ModelCollapse
          name={name}
          toggleCollapsed={this.toggleCollapsed}
          isCollapsed={isCollapsed}
          self={true}
        >
          <ModelWrapper
            values={values}
            oneOf={oneOf}
            name={name}
            fields={fields}
            spec={this.props.spec}
            isCollapsed={isCollapsed}
            toggleCollapsed={this.toggleCollapsed}
          />
        </ModelCollapse>
      </div>
    )
  }
}

class ModelCollapse extends Component {
  render () {
    const { toggleCollapsed, isCollapsed, name, self } = this.props
    const title = (
      <span className="model-box">
        <span className="model model-title">{name}</span>
      </span>
    )
    if (!isCollapsed && self) {
      return <span className="model-box">{this.props.children}</span>
    }
    return (
      <span className="model-box">
        <span onClick={toggleCollapsed} className="pointer">
          {title}
        </span>
        <span onClick={toggleCollapsed} className="pointer">
          <span
            className={'model-toggle' + (isCollapsed ? '' : ' collapsed')}
          ></span>
        </span>
        {!isCollapsed ? this.props.children : ' '}
      </span>
    )
  }
}

class ModelWrapper extends Component {
  render () {
    return (
      <div className="model-box">
        <ObjectModel {...this.props} />
      </div>
    )
  }
}

class PrimitiveModel extends Component {
  render () {
    const { type, schema, label } = this.props.field
    let displayType = type.toLowerCase()
    let isArray = label === 'repeated'
    if (schema) {
      const subSchema = this.props.spec.messages.find(
        sub => sub.name === schema && isMap(sub)
      )
      if (subSchema) {
        isArray = false
        displayType = `map<${
          subSchema.fields.find(f => f.name === 'key').type
        },${subSchema.fields.find(f => f.name === 'value').type}>`
      } else {
        displayType = schema
      }
    }
    return (
      <span className="model">
        <span className="prop">
          <span className="prop-type">
            {isArray && bracketOpen}
            {displayType}
            {isArray && bracketClose}
          </span>
        </span>
      </span>
    )
  }
}

class ObjectModel extends Component {
  render () {
    const {
      isCollapsed,
      toggleCollapsed,
      spec,
      values,
      oneOf,
      fields,
      name,
      isOneOf
    } = this.props
    const isObject = !!fields

    const enums = Object.values(values || {}).map(value => (
      <tr key={`value-${value}`} className="property-row">
        <td>{value}</td>
      </tr>
    ))

    const oneOfs = Object.entries(oneOf || {}).map(([name, fields]) => {
      const classNames = ['property-row']
      const key = `field-${name}`
      return (
        <tr key={key} className={classNames.join(' ')}>
          <td>{name}</td>
          <td>
            <ObjectModel
              key={`object_${key}`}
              isOneOf={true}
              fields={fields}
              spec={spec}
              name={name}
              isCollapsed={isCollapsed}
            />
          </td>
        </tr>
      )
    })

    const entries = (fields || []).map(field => {
      const classNames = ['property-row']
      const isRequired = field.label === 'required'

      if (isDeprecated(field)) {
        classNames.push('deprecated')
      }

      if (isRequired) {
        classNames.push('required')
      }

      const key = `field-${field.name}`

      return (
        <tr key={key} className={classNames.join(' ')}>
          <td>
            {field.name} {isRequired && <span className="star">*</span>}
          </td>
          <td>
            <PrimitiveModel key={`object_${key}`} field={field} spec={spec} />
          </td>
        </tr>
      )
    })

    const body = (
      <div>
        {isOneOf && <span className="prop-type">oneOf{' '}</span>}
        <span className="brace-open object">
          {isObject ? braceOpen : bracketOpen}
        </span>
        <span className="inner-object">
          <table className="model">
            <tbody>
              {entries}
              {oneOfs}
              {enums}
            </tbody>
          </table>
        </span>
        <span className="brace-close">
          {isObject ? braceClose : bracketClose}
        </span>
      </div>
    )

    return (
      <span className="model">
        {!isOneOf ? (
          <ModelCollapse
            name={name}
            toggleCollapsed={toggleCollapsed}
            isCollapsed={isCollapsed}
          >
            {body}
          </ModelCollapse>
        ) : (
          body
        )}
      </span>
    )
  }
}
