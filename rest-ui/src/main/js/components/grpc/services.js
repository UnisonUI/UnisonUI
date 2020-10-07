import React, { Component } from 'react'
import { Link, Collapse } from './utils'
import  Method  from './method'
import { ChevronDown, ChevronRight } from 'react-feather'

export default class Services extends Component {
  render () {
    const { id, server, schema } = this.props
    const services = schema.services.map(service => (
      <Service
        key={`service-${service.fullName}`}
        service={service}
        schema={schema}
        server={server}
        id={id}
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
    const { id, server, service, schema } = this.props
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
              service={service.fullName}
              server={server}
              schema={schema}
              method={method}
              id={id}
            />
          ))}
        </Collapse>
      </div>
    )
  }
}
