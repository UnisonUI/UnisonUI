import PropTypes from 'prop-types'
import React, { Component } from 'react'
import { withRouter } from 'react-router-dom'
import SwaggerUI from 'swagger-ui-react'
import NoService from './noService'

class Swagger extends Component {
  render () {
    const name = this.props.location.pathname.substring(1)
    return (
      <div>
        {name ? (
          <SwaggerUI url={`/services/${name}`} docExpansion="list" />
        ) : (
          <NoService />
        )}
      </div>
    )
  }
}

Swagger.propTypes = {
  match: PropTypes.object.isRequired,
  location: PropTypes.object.isRequired,
  history: PropTypes.object.isRequired
}

export default withRouter(Swagger)
