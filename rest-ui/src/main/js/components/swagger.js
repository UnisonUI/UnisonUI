import PropTypes from 'prop-types'
import React, { Component } from 'react'
import { withRouter } from 'react-router-dom'
import NoService from './noService'
import SwaggerUI from 'swagger-ui-react'

const theme = () => {
  return { syntaxHighlight: { activated: true, theme: 'obsidian' } }
}

const RestUILayoutPlugin = system => {
  return {
    components: {
      InfoUrl: () => null
    },
    wrapComponents: {
      curl: (Original, system) => props => {
        const url = atob(props.request.get('url').substring(7))
        const request = props.request.set('url', url)
        return <Original request={request} getConfigs={props.getConfigs} />
      },
      highlightCode: (Original, system) => props => {
        return <Original {...props} getConfigs={theme} />
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
            plugins={[RestUILayoutPlugin]}
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
