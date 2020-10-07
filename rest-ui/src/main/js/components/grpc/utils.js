import PropTypes from 'prop-types'
import React, { Component } from 'react'

function xclass (...args) {
  return args.filter(a => !!a).join(' ').trim()
}

const DEVICES = {
  mobile: '',
  tablet: '-tablet',
  desktop: '-desktop',
  large: '-hd'
}

export class Col extends React.Component {
  render () {
    const {
      hide,
      keepContents,
      /* eslint-disable no-unused-vars */
      mobile,
      tablet,
      desktop,
      large,
      /* eslint-enable no-unused-vars */
      ...rest
    } = this.props

    if (hide && !keepContents) return <span />

    const classesAr = []

    for (const device in DEVICES) {
      /* eslint-disable no-prototype-builtins */
      if (!DEVICES.hasOwnProperty(device)) {
        continue
      }
      /* eslint-enable no-prototype-builtins */
      const deviceClass = DEVICES[device]
      if (device in this.props) {
        const val = this.props[device]

        if (val < 1) {
          classesAr.push('none' + deviceClass)
          continue
        }

        classesAr.push('block' + deviceClass)
        classesAr.push('col-' + val + deviceClass)
      }
    }

    if (hide) {
      classesAr.push('hidden')
    }

    const classes = xclass(rest.className, ...classesAr)

    return <section {...rest} className={classes} />
  }
}

Col.propTypes = {
  hide: PropTypes.bool,
  keepContents: PropTypes.bool,
  mobile: PropTypes.number,
  tablet: PropTypes.number,
  desktop: PropTypes.number,
  large: PropTypes.number,
  className: PropTypes.string
}

export class Row extends React.Component {
  render () {
    return (
      <div
        {...this.props}
        className={xclass(this.props.className, 'wrapper')}
      />
    )
  }
}

Row.propTypes = {
  className: PropTypes.string
}

export class Button extends React.Component {
  render () {
    return (
      <button
        {...this.props}
        className={xclass(this.props.className, 'button')}
      />
    )
  }
}
Button.propTypes = {
  className: PropTypes.string
}
Button.defaultProps = {
  className: ''
}

export class Collapse extends Component {
  render () {
    let { isOpened, children } = this.props
    children = isOpened ? children : null
    return <NoMargin>{children}</NoMargin>
  }
}

const NoMargin = ({ children }) => <div className='no-margin'> {children} </div>

NoMargin.propTypes = {
  children: PropTypes.node
}

Collapse.propTypes = {
  isOpened: PropTypes.bool,
  children: PropTypes.node.isRequired
}

Collapse.defaultProps = {
  isOpened: false
}
