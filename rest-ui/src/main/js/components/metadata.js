import React from 'react'

export default function Metadata (props) {
  let className = 'metadata'
  if (props.isOpen) {
    className += ' open'
  }

  const items = Object.entries(props.metadata)
    .map(([name, value], index) => {
      return <li key={index}> {`${name} = ${value}`} </li>
    })
  return <ul className={className}> {items} </ul>
}
