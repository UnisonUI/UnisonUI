import React, { Component } from 'react'
import { HashRouter as Router } from 'react-router-dom'
import Menu from 'react-burger-menu/lib/menus/push'
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
      services: []
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

  getServices () {
    if (this.state.services.length) {
      const items = this.state.services.map((service, index) => {
        return (
          <div key={index + 1}>
            <ServiceLink service={service} closeMenu={() => this.closeMenu()} />
          </div>
        )
      })
      items.unshift(<h1 key="0">List of services</h1>)
      return items
    } else {
      return [<h1 key="0">No service available</h1>]
    }
  }

  render () {
    return (
      <div id="outer-container">
        <Konami
          action={() => cornify.pizzazz()}
          timeout="15000"
          onTimeout={() => cornify.clear()}
        />
        <Router>
          <div id="outer-container">
            <Menu
              pageWrapId="page-wrap"
              outerContainerId="outer-container"
              isOpen={this.state.menuOpen}
              onStateChange={state => this.handleStateChange(state)}
            >
              {this.getServices()}
            </Menu>
          </div>
          <main id="page-wrap">
            <SwaggerWithRouter />
          </main>
        </Router>
      </div>
    )
  }
}
