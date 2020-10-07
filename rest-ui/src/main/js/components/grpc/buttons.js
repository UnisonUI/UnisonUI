import React, { Component } from 'react'

export class Execute extends Component {
  constructor (props) {
    super(props)
    this.onClick = this.onClick.bind(this)
  }

  onClick () {
    this.props.onExecute()
  }

  render () {
    return (
      <button
        className="btn execute opblock-control__btn"
        onClick={this.onClick}
      >
        Execute
      </button>
    )
  }
}

export class Clear extends Component {
  constructor (props) {
    super(props)
    this.onClick = this.onClick.bind(this)
  }

  onClick () {
    this.props.onClear()
  }

  render () {
    return (
      <button
        className="btn btn-clear opblock-control__btn"
        onClick={this.onClick}
      >
        Clear
      </button>
    )
  }
}
