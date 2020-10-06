import React, { Component } from 'react'
import Servers from './servers'
import Models from './models'
import { Row, Col } from './utils'

export default class Layout extends Component {
  render () {
    const { servers, schema } = this.props.spec
    return (
      <div className='swagger-ui'>
        <Info title={this.props.title} />
        <Servers servers={servers} />
        <Row>
          <Col mobile={12} desktop={12}>
            <Models spec={schema} />
          </Col>
        </Row>
      </div>
    )
  }
}
const Info = (props) => (
  <Row className='information-container'>
    <Col mobile={12}>
      <div className='info'>
        <hgroup className='main'>
          <h2 className='title'>
            {props.title}{' '}
            <small className='version-stamp'>
              <pre className='version'>gRPC</pre>
            </small>
          </h2>
        </hgroup>
      </div>
    </Col>
  </Row>
)
