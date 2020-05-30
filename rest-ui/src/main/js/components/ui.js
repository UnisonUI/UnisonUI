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
      services: [],
      filtered: []
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
      const services = res.data.map(event => {
        return { id: event.id, name: event.name, metadata: event.metadata }
      })
      services.sort((a, b) => a.name.localeCompare(b.name))
      this.setState({ services, filtered: services })
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
      if (!services.find(item => item.id === data.id)) {
        services.push({ id: data.id, name: data.name, metadata: data.metadata })
      }
    } else {
      services = this.state.services.filter(item => item.id !== data.id)
    }
    services.sort((a, b) => a.name.localeCompare(b.name))
    this.setState({ services, filtered: services })
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

    if (services.length) {
      services.forEach(service => {
        items.push(
          <div key={service.id}>
            <ServiceLink service={service} closeMenu={() => this.closeMenu()} />
          </div>
        )
      })
    } else {
      items.push(<h1 key="0">No services available</h1>)
    }
    return items
  }

  search (e) {
    let currentList = []
    let newList = []

    if (e.target.value !== '') {
      currentList = this.state.services

      newList = currentList.filter(service => {
        const lc = service.name.toLowerCase()
        const filter = e.target.value.toLowerCase()
        return lc.includes(filter)
      })
    } else {
      newList = this.state.services
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
