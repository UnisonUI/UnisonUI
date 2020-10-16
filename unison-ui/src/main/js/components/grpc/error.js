import React, { Component } from 'react'
import { Collapse } from './utils'

export default class Error extends Component {
  constructor (props) {
    super(props)
    this.state = { isVisible: true }
    this.toggleVisibility = this.toggleVisibility.bind(this)
  }

  toggleVisibility () {
    this.setState({ isVisible: !this.state.isVisible })
  }

  render () {
    return (
      <pre className="errors-wrapper">
        <hgroup className="error">
          <h4 className="errors__title">Errors</h4>
          <button
            className="btn errors__clear-btn"
            onClick={this.toggleVisibility}
          >
            {this.state.isVisible ? 'Hide' : 'Show'}
          </button>
        </hgroup>
        <Collapse isOpened={this.state.isVisible}>
          <div className="errors">
            <div className="error-wrapper">
              <div>
                <span className="message thrown">{this.props.message}</span>
              </div>
            </div>
          </div>
        </Collapse>
      </pre>
    )
  }
}
