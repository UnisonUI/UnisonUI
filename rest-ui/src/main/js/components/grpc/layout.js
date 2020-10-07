import React, { Component } from 'react'
import Servers from './servers'
import Services from './services'
import Models from './models'
import { Row, Col } from './utils'

export default class Layout extends Component {
  constructor (props) {
    super(props)
    this.state = { server: props.spec.servers[0] }
    this.setServer = this.setServer.bind(this)
  }

  setServer (e) {
    this.setState({ server: e.target.value })
  }

  render () {
    const { servers, schema } = this.props.spec
    return (
      <div className="swagger-ui">
        <Info title={this.props.title} />
        <Servers servers={servers} onChange={this.setServer} />
        <Row>
          <Col mobile={12} desktop={12}>
            <Services schema={schema} server={this.state.server} id={this.props.id} />
          </Col>
        </Row>
        <Row>
          <Col mobile={12} desktop={12}>
            <Models spec={schema} />
          </Col>
        </Row>
      </div>
    )
  }
}
const Info = props => (
  <Row className="information-container">
    <Col mobile={12}>
      <div className="info">
        <hgroup className="main">
          <h2 className="title">
            {props.title}{' '}
            <small className="version-stamp">
              <pre className="version">gRPC</pre>
            </small>
          </h2>
        </hgroup>
      </div>
    </Col>
  </Row>
)
