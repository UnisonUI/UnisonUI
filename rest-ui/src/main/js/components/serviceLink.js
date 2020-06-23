import React, { Component } from 'react'
import { NavLink } from 'react-router-dom'

export default class ServiceLink extends Component {
  constructor (props) {
    super(props)
    this.state = {
      metadataOpen: false
    }
  }

  render () {
    const services = this.props.services
    if (services.length === 1) {
      return (
        <NavLink
          key={services[0].id}
          to={`/${services[0].id}`}
          onClick={this.props.closeMenu}
        >
          {services[0].name}
        </NavLink>
      )
    } else {
      services.sort((a, b) => a.name.localeCompare(b.name))
      const items = services.map(service => (
        <div key={service.id} className="text-sm ml-4">
          <NavLink to={`/${service.id}`} onClick={this.props.closeMenu}>
            {service.metadata.file}
          </NavLink>
        </div>
      ))
      return (
        <div>
          <div>{services[0].name}</div>
          {items}
        </div>
      )
    }
  }
}
