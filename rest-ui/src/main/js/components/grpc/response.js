import React from 'react'
import { HighlightCode } from './highlight-code'

export const Response = ({ response }) => {
  const { status, data } = response
  return (
    <div>
      <h4>Server response</h4>
      <table className='responses-table live-responses-table'>
        <thead>
          <tr className='responses-header'>
            <td className='col_header response-col_status'>Code</td>
            <td className='col_header response-col_description'>Details</td>
          </tr>
        </thead>
        <tbody>
          <tr className='response'>
            <td className='response-col_status'>{status}</td>
            <td className='response-col_description'>
              <h5>Response body</h5>
              <HighlightCode code={data} />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}
