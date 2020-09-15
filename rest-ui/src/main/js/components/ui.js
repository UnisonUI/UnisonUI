import loadable from '@loadable/component'
import { createBrowserHistory } from 'history'
import React, { Component } from 'react'
import { HashRouter as Router } from 'react-router-dom'
import Menu from 'react-burger-menu/lib/menus/push'
import axios from 'axios'
import * as cornify from '../cornified'

import Konami from 'react-konami-code'
const SwaggerWithRouter = loadable(() => import('./swagger'))
const ServiceLink = loadable(() => import('./serviceLink'))
const history = createBrowserHistory()

export default class App extends Component {
  constructor (props) {
    super(props)
    this.state = {
      menuOpen: false,
      services: {},
      filtered: {}
    }
    this.eventSource = new EventSource('/events')
  }

  handleStateChange (state) {
    this.setState({ menuOpen: state.isOpen })
  }

  closeMenu () {
    this.setState({ menuOpen: false })
  }

  componentDidMount () {
    axios.get('/services').then(res => {
      const services = res.data
        .map(event => {
          return { id: event.id, name: event.name, metadata: event.metadata }
        })
        .reduce((obj, service) => {
          if (!obj[service.name]) {
            obj[service.name] = []
          }
          obj[service.name].push(service)
          return obj
        }, {})
      this.setState({ services })
      this.search(document.getElementById('search').value)
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
      if (!services[data.name]) {
        services[data.name] = []
      }

      if (!services[data.name].find(item => item.id === data.id)) {
        services[data.name].push({
          id: data.id,
          name: data.name,
          metadata: data.metadata
        })
      } else {
        services[data.name] = services[data.name].map(service => {
          if (service.id !== data.id) return service
          else return { id: data.id, name: data.name, metadata: data.metadata }
        })
      }
    } else {
      services = Object.entries(this.state.services).reduce(
        (obj, [name, services]) => {
          const filteredServices = services.filter(item => item.id !== data.id)
          if (filteredServices.length) {
            obj[name] = filteredServices
          }
          return obj
        },
        {}
      )
    }
    this.setState({ services })
    this.search(document.getElementById('search').value)
  }

  getServices () {
    const services = this.state.filtered
    const items = [
      <input
        type="text"
        id="search"
        className="search"
        placeholder="Search for a service..."
        onChange={e => this.search(e.target.value)}
        key="_search"
      />
    ]

    if (Object.keys(services).length) {
      const entries = Object.entries(services)
      entries.sort((a, b) => a[0].localeCompare(b[0]))
      entries.forEach(([name, services]) => {
        items.push(
          <div key={name} className={this.getNavLinkClass(services)}>
            <ServiceLink
              services={services}
              closeMenu={() => this.closeMenu()}
            />
          </div>
        )
      })
    } else {
      items.push(<h1 key="0" style={{ padding: '0.5em' }}>No services available</h1>)
    }
    return items
  }

  search (input) {
    let newList = {}

    if (input !== '') {
      const keys = Object.keys(this.state.services)

      newList = keys
        .filter(name => {
          const lc = name.toLowerCase()
          const filter = input.toLowerCase()
          return lc.includes(filter)
        })
        .reduce((obj, name) => {
          obj[name] = this.state.services[name]
          return obj
        }, {})
    } else {
      newList = Object.assign({}, this.state.services)
    }
    this.setState({
      filtered: newList
    })
  }

  getNavLinkClass (services) {
    return services.some(
      service => history.location.hash === `#/${service.id}`
    )
      ? 'active'
      : ''
  }

  render () {
    return (
      <div id="outer-container" style={{ height: '100%' }}>
        <Konami
          action={() => cornify.pizzazz()}
          timeout="15000"
          onTimeout={() => cornify.clear()}
        />
        <Router>
          <Menu
            pageWrapId={'page-wrap'}
            outerContainerId={'outer-container'}
            isOpen={this.state.menuOpen}
            onStateChange={state => this.handleStateChange(state)}
          >
            {this.getServices()}
          </Menu>
          <main id="page-wrap">
            <SwaggerWithRouter />
          </main>
        </Router>
      </div>
    )
  }
}
