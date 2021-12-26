import loadable from '@loadable/component'
import { Moon, Sun } from 'react-feather'
import { createBrowserHistory } from 'history'
import React, { Component } from 'react'
import { HashRouter as Router } from 'react-router-dom'
import Menu from 'react-burger-menu/lib/menus/push'
import ServiceLink from './serviceLink'
import axios from 'axios'
import * as cornify from '../cornified'
import Konami from 'react-konami-code'
import NoService from './noService'

const OpenAPI = loadable(() => import('./openapi'))
const GRPC = loadable(() => import('./grpc'))
const history = createBrowserHistory()

export default class App extends Component {
  constructor(props) {
    super(props)
    this.state = {
      menuOpen: false,
      darkMode: localStorage.getItem('darkMode') === 'true',
      services: {},
      filtered: {}
    }
    this._toggleTheme = this._toggleTheme.bind(this)
  }

  handleStateChange(state) {
    this.setState({ menuOpen: state.isOpen })
  }

  closeMenu() {
    this.setState({ menuOpen: false })
  }

  _toggleTheme() {
    const newTheme = !this.state.darkMode
    localStorage.setItem('darkMode', newTheme)
    this.setState({ darkMode: newTheme })
  }

  _connect() {
    const websocket = new WebSocket(
      `ws${location.protocol.replace('http', '')}//${location.host
      }/ws`
    )

    websocket.onclose = _ => {
      setTimeout(() => this._connect(), 1000)
    }

    websocket.onmessage = e => {
      if (e.data) {
        this.handleEndpoint(JSON.parse(e.data))
      }
    }
  }

  componentDidMount() {
    axios.get('/services').then(res => {
      const services = res.data
        .map(event => {
          return {
            id: event.id,
            name: event.name,
            metadata: event.metadata,
            useProxy: event.useProxy,
            type: event.type
          }
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
    this._connect()
  }

  handleEndpoint(data) {
    let services = this.state.services
    switch (data.event) {
      case 'serviceUp':
        if (!services[data.name]) {
          services[data.name] = []
        }

        if (!services[data.name].find(item => item.id === data.id)) {
          services[data.name].push({
            id: data.id,
            name: data.name,
            metadata: data.metadata,
            useProxy: data.useProxy,
            type: data.type
          })
        } else {
          services[data.name] = services[data.name].map(service => {
            if (service.id !== data.id) return service
            else {
              return {
                id: data.id,
                name: data.name,
                metadata: data.metadata,
                useProxy: data.useProxy,
                type: data.type
              }
            }
          })
        }
        break
      case 'serviceDown':
        services = Object.entries(this.state.services).reduce(
          (obj, [name, services]) => {
            const filteredServices = services.filter(
              item => item.id !== data.id
            )
            if (filteredServices.length) {
              obj[name] = filteredServices
            }
            return obj
          },
          {}
        )
        break
      case 'serviceChanged':
        if (`#/${data.id}` === history.location.hash) {
          return history.go(0)
        }
        break
    }
    this.setState({ services })
    this.search(document.getElementById('search').value)
  }

  getServices() {
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
      items.push(
        <h1 key="0" style={{ padding: '0.5em' }}>
          No services available
        </h1>
      )
    }
    return items
  }

  search(input) {
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

  getNavLinkClass(services) {
    return services.some(service => history.location.hash === `#/${service.id}`)
      ? 'active'
      : ''
  }

  render() {
    const service = Object.values(this.state.services)
      .flat()
      .find(service => history.location.hash === `#/${service.id}`)
    let useProxy = false
    let type = null
    if (service) {
      useProxy = service.useProxy
      type = service.type
    }
    return (
      <div
        id="outer-container"
        style={{ height: '100%' }}
        className={this.state.darkMode ? 'dark' : ''}
      >
        <button className="themeSwitch" onClick={this._toggleTheme}>
          {this.state.darkMode ? <Sun size={42} /> : <Moon size={42} />}
        </button>
        <Konami
          action={() => cornify.pizzazz()}
          timeout={15000}
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
            {service
              ? (
                type === 'openapi'
                  ? (
                    <OpenAPI useProxy={useProxy} />
                  )
                  : (
                    <GRPC title={service.name} />
                  )
              )
              : (
                <NoService />
              )}
          </main>
        </Router>
      </div>
    )
  }
}
