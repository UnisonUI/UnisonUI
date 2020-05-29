import React, { Component } from 'react'
import { NavLink } from 'react-router-dom'
import axios from 'axios'

export default class Services extends Component {
  constructor (props) {
    super(props)
    this.state = {
      services: []
    }
    this.eventSource = new EventSource('/events')
  }

  componentDidMount () {
    axios.get('/services').then(res => {
      const services = res.data.map(event => {
        return { name: event.name, metadata: event.metadata }
      })
      services.sort((a, b) => a.name.localeCompare(b.name))
      this.setState({ services })
    })
    this.eventSource.onmessage = e => {
      if (e.data) {
        this.handleEndpoint(JSON.parse(e.data))
      }
    }
  }

  handleEndpoint (data) {
    let services
    if (data.event === 'serviceUp') {
      services = this.state.services
      if (!services.find(item => item.name === data.name)) {
        services.push({ name: data.name, metadata: data.metadata })
      }
    } else {
      services = this.state.services.filter(item => item.name !== data.name)
    }
    services.sort((a, b) => a.name.localeCompare(b.name))
    this.setState({ services })
  }

  render () {
    if (this.state.services.length) {
      return (
        <section>
          <h1>List of services</h1>
          <ul className="items">
            {this.state.services.map(service => {
              let metadata = ''
              const obj = Object.values(service.metadata)
              if (obj.length) {
                metadata = ` (${obj.join(', ')})`
              }
              return (
                <li key={service.name}>
                  <NavLink to={`/${service.name}`} activeClassName="active">
                    {`${service.name}${metadata}`}
                  </NavLink>
                </li>
              )
            })}
          </ul>
        </section>
      )
    } else {
      return <h1>No service available</h1>
    }
  }
}
