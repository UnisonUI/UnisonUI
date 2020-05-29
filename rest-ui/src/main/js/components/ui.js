import PropTypes from 'prop-types'
import React, { Component } from 'react'
import { HashRouter as Router, withRouter } from 'react-router-dom'
import SwaggerUI from 'swagger-ui-react'
import Services from './services'

require('swagger-ui-react/swagger-ui.css')

export default function App () {
  return (
    <Router>
      <nav className='menu'>
        <Services />
      </nav>
      <SwaggerWithRouter />
    </Router>
  )
}

class Swagger extends Component {
  render () {
    const name = this.props.location.pathname.substring(1)
    return (
      <main>
        {name ? (
          <SwaggerUI url={`/services/${name}`} docExpansion='list' />
        ) : (
          <div />
        )}
      </main>
    )
  }
}

Swagger.propTypes = {
  match: PropTypes.object.isRequired,
  location: PropTypes.object.isRequired,
  history: PropTypes.object.isRequired
}

const SwaggerWithRouter = withRouter(Swagger)
