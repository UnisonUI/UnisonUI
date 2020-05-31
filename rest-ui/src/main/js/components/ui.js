import React, { Component } from 'react'
import { HashRouter as Router } from 'react-router-dom'
import Menu from 'react-burger-menu/lib/menus/pushRotate'
import { Menu as FeatherMenu, XSquare } from 'react-feather'
import axios from 'axios'
import Konami from 'react-konami-code'
import * as cornify from '../cornified'

import SwaggerWithRouter from './swagger'
import ServiceLink from './serviceLink'

export default class App extends Component {
  constructor (props) {
    super(props)
    this.state = {
      menuOpen: false,
      services: {},
      filtered: {}
    }
    this.eventSource = new EventSource('/events')
    this.search = this.search.bind(this)
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
      this.setState({ services, filtered: Object.assign({}, services) })
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
      services = Object.assign({}, this.state.services)
      const service = services[data.name]
      if (service) {
        services[data.name] = services[data.name].filter(
          item => item.id !== data.id
        )
      }
      if (!services[data.name].length) {
        delete services[data.name]
      }
    }
    this.setState({ services, filtered: Object.assign({}, services) })
  }

  getServices () {
    const services = this.state.filtered
    const items = [
      <input
        type="text"
        className="search"
        placeholder="Search for a service..."
        onChange={this.search}
        key="_search"
      />
    ]

    if (Object.keys(services).length) {
      const entries = Object.entries(services)
      entries.sort((a, b) => a[0].localeCompare(b[0]))
      entries.forEach(([name, services]) => {
        items.push(
          <div key={name}>
            <ServiceLink
              services={services}
              closeMenu={() => this.closeMenu()}
            />
          </div>
        )
      })
    } else {
      items.push(<h1 key="0">No services available</h1>)
    }
    return items
  }

  search (e) {
    let newList = {}

    if (e.target.value !== '') {
      const keys = Object.keys(this.state.services)

      newList = keys
        .filter(name => {
          const lc = name.toLowerCase()
          const filter = e.target.value.toLowerCase()
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
            customBurgerIcon={<FeatherMenu size={48} />}
            customCrossIcon={<XSquare size={48} color="#e2e8f0" />}
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
