import PropTypes from 'prop-types'
import React, { Component } from 'react'
import { withRouter } from 'react-router-dom'
import SwaggerUI from 'swagger-ui-react'
import NoService from './noService'

class Swagger extends Component {
  render () {
    const id = this.props.location.pathname.substring(1)
    return (
      <div>
        {id ? (
          <SwaggerUI url={`/services/${id}`} docExpansion="list" />
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
