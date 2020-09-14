import PropTypes from 'prop-types'
import React, { Component } from 'react'
import { withRouter } from 'react-router-dom'
import NoService from './noService'
import SwaggerUI from 'swagger-ui-react'

const RestUILayoutPlugin = () => {
  return {
    components: {
      InfoUrl: () => null
    }
  }
}

const CurlPlugin = function (system) {
  return {
    wrapComponents: {
      curl: (Original, system) => props => {
        const url = atob(props.request.get('url').substring(7))
        const request = props.request.set('url', url)
        return <Original request={request} getConfigs={props.getConfigs} />
      }
    }
  }
}

class Swagger extends Component {
  render () {
    const id = this.props.location.pathname.substring(1)
    const requestInterceptor = r => {
      if (r.url.startsWith('http')) {
        r.url = `/proxy/${btoa(r.url)}`
      }
      return r
    }
    return (
      <div>
        {id ? (
          <SwaggerUI
            url={'/services/' + id}
            requestInterceptor={requestInterceptor}
            plugins={[RestUILayoutPlugin, CurlPlugin]}
          />
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
