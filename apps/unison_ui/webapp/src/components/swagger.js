import PropTypes from 'prop-types'
import React, { Component } from 'react'
import { useLocation } from 'react-router-dom'
import SwaggerUI from 'swagger-ui-react'

const theme = () => {
  return { syntaxHighlight: { activated: true, theme: 'obsidian' } }
}

const ProxyPlugin = system => {
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
const RestUILayoutPlugin = system => {
  return {
    components: {
      InfoUrl: () => null
    },
    wrapComponents: {
      highlightCode: (Original, system) => props => {
        return <Original {...props} getConfigs={theme} />
      }
    }
  }
}

class Swagger extends Component {
  render() {
    const location = useLocation()
    const useProxy = !!this.props.useProxy
    const id = this.props.location.pathname.substring(1)
    const plugins = [RestUILayoutPlugin]
    if (useProxy) plugins.push(ProxyPlugin)
    const requestInterceptor = r => {
      if (useProxy && r.url.startsWith('http')) {
        r.url = `/proxy/${btoa(r.url)}`
      }
      return r
    }
    return (
      <div>
        <SwaggerUI
          url={'/services/' + id}
          requestInterceptor={requestInterceptor}
          plugins={plugins}
        />
      </div>
    )
  }
}

export default Swagger
