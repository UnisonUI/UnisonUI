import React, { Component } from 'react'
import { Clear, Execute } from './buttons'
import Request from './request'
import { Response } from './Response'
import { Link, Collapse } from './utils'
import axios from 'axios'
const CancelToken = axios.CancelToken

const TryItOut = ({ onTryoutClick, onCancelClick, enabled }) => (
  <div className="try-out">
    {enabled ? (
      <button className="btn try-out__btn cancel" onClick={onCancelClick}>
        Cancel
      </button>
    ) : (
      <button className="btn try-out__btn" onClick={onTryoutClick}>
        Try it out
      </button>
    )}
  </div>
)

export default class Method extends Component {
  constructor (props) {
    super(props)
    this.state = {
      value: null,
      isVisible: false,
      tryItOutEnabled: false,
      executeInProgress: false,
      response: null,
      cancel: CancelToken.source()
    }
    this.toggleVisibility = this.toggleVisibility.bind(this)
    this.onTryoutClick = this.onTryoutClick.bind(this)
    this.onCancelClick = this.onCancelClick.bind(this)
    this.onExecute = this.onExecute.bind(this)
    this.onClear = this.onClear.bind(this)
    this.onUpdate = this.onUpdate.bind(this)
  }

  onUpdate (value) {
    this.setState({ value })
  }

  onClear () {
    this.setState({ executeInProgress: false })
  }

  onExecute () {
    let { id, method, service, server } = this.props
    id = btoa(id).replace('/', '_')
    server = server.name
    let data = {}
    try {
      data = JSON.parse(this.state.value)
    } catch (e) {}
    this.setState({ executeInProgress: true })
    axios
      .post(
        `/grpc/${id}/${service}/${method.name}`,
        { server, data },
        { cancelToken: this.state.cancel.token }
      )
      .then(response => this.setState({ executeInProgress: false, response }))
      .catch(error => {
        let response
        if (axios.isCancel(error)) {
          response = null
        } else {
          response = error.response
        }
        this.setState({ executeInProgress: false, response: response })
      })
  }

  toggleVisibility () {
    this.setState({ isVisible: !this.state.isVisible })
  }

  onTryoutClick () {
    this.setState({ tryItOutEnabled: true })
  }

  onCancelClick () {
    this.state.cancel.cancel()
    this.setState({ tryItOutEnabled: false })
  }

  render () {
    const methodName = 'post'
    const { method, schema } = this.props
    const {
      tryItOutEnabled,
      isVisible,
      executeInProgress,
      response
    } = this.state
    return (
      <div
        className={
          isVisible
            ? `opblock opblock-${methodName} is-open`
            : `opblock opblock-${methodName}`
        }
      >
        <div
          className={`opblock-summary opblock-summary-${methodName}`}
          onClick={this.toggleVisibility}
        >
          <span className="opblock-summary-method">RPC</span>
          <span className="opblock-summary-path">
            <Link text={method.name} />
          </span>
        </div>
        <Collapse isOpened={isVisible}>
          <div className="opblock-body">
            <div className="opblock-section opblock-section-request-body">
              <div className="opblock-section-header">
                <h4 className="opblock-title parameter__name">
                  Request (<small>{method.inputType}</small>)
                </h4>
                <TryItOut
                  enabled={tryItOutEnabled}
                  onCancelClick={this.onCancelClick}
                  onTryoutClick={this.onTryoutClick}
                />
              </div>
              <div className="opblock-description-wrapper">
                <Request
                  schema={schema}
                  method={method.inputType}
                  isExecute={tryItOutEnabled}
                  onUpdate={this.onUpdate}
                />
              </div>
            </div>
            <div
              className={
                !tryItOutEnabled || !response ? 'execute-wrapper' : 'btn-group'
              }
            >
              {!tryItOutEnabled ? null : <Execute onExecute={this.onExecute} />}

              {!tryItOutEnabled || !response ? null : (
                <Clear onClear={this.onClear} />
              )}
            </div>

            {executeInProgress ? (
              <div className="loading-container">
                <div className="loading"></div>
              </div>
            ) : null}
            <div className="responses-wrapper">
              <div className="opblock-section-header">
                <h4>
                  Response (<small>{method.outputType}</small>)
                </h4>
              </div>
              <div className="responses-inner">
                {response ? <Response response={response} /> : null}
              </div>
            </div>
          </div>
        </Collapse>
      </div>
    )
  }
}
