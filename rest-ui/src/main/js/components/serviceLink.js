import React, { Component } from 'react'
import { NavLink } from 'react-router-dom'
import { ChevronDown, ChevronUp } from 'react-feather'

import Metadata from './metadata'

export default class ServiceLink extends Component {
  constructor (props) {
    super(props)
    this.state = {
      metadataOpen: false
    }
  }

  render () {
    const service = this.props.service
    const metadata = Object.assign({},service.metadata)
    let button = []
    let metadataChild = []
    const provider = metadata.provider
    delete metadata.provider

    if (Object.values(metadata).length) {
      let chevron = <ChevronDown size={16} />
      if (this.state.metadataOpen) {
        chevron = <ChevronUp size={16} />
      }

      button = [
        <button
          key="0"
          onClick={() =>
            this.setState({ metadataOpen: !this.state.metadataOpen })
          }
        >
          {chevron}
        </button>
      ]
      metadataChild = [
        <Metadata
          key="2"
          isOpen={this.state.metadataOpen}
          metadata={metadata}
        />
      ]
    }
    return (
      <div>
        {button}
        <NavLink
          key="1"
          to={`/${service.name}`}
          activeClassName="active"
          onClick={this.props.closeMenu}
        >
          {service.name}{' '}
          <i
            className={`devicon-${provider}-plain-wordmark`}
          ></i>
        </NavLink>
        {metadataChild}
      </div>
    )
  }
}
