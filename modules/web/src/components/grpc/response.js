import React from 'react'
import { HighlightCode } from './highlight-code'

export const Response = ({ response }) => {
  let status = 200
  if (Array.isArray(response)) {
    const latest = response.find(r => !!r.status)
    status = latest ? latest.status : status
  } else status = response.status || status
  return (
    <div>
      <h4>Server response</h4>
      <table className="responses-table live-responses-table">
        <thead>
          <tr className="responses-header">
            <td className="col_header response-col_status">Code</td>
            <td className="col_header response-col_description">Details</td>
          </tr>
        </thead>
        <tbody>
          <tr className="response">
            <td className="response-col_status">{status}</td>
            <td className="response-col_description">
              {Array.isArray(response)
                ? (
                    response.map((resp, i) => (
                  <div key={'response' + i}>
                    <h5>{resp.data ? 'Response' : 'Request'}</h5>
                    <HighlightCode code={resp.data || resp.value} />
                  </div>
                    ))
                  )
                : (
                <div>
                  <h5>Response</h5>
                  <HighlightCode code={response.data} />
                </div>
                  )}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}
