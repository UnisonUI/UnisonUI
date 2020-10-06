import React, { Component } from 'react'
import { Col } from './utils'
export default class Servers extends Component {
  render () {
    return (
      <div className="scheme-container">
        <Col className="schemes wrapper" mobile={12}>
          <div>
            <span className="servers-title">Servers</span>
            <div className="servers">
              <label htmlFor="servers">
                <select>
                  {this.props.servers.map(server => (
                    <option value={server.name} key={server.name}>
                      {server.name} - {server.useTls ? '(secure)' : '(insecure)'}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </div>
        </Col>
      </div>
    )
  }
}

class Server extends Component {
  render () {}
}
