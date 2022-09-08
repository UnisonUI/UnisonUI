import React, { Component } from "react";
import { Col } from "./utils";

export default class Servers extends Component {
  render() {
    const { servers, onChange } = this.props;
    return (
      <div className="scheme-container">
        <Col className="schemes wrapper" mobile={12}>
          <div>
            <span className="servers-title">Servers</span>
            <div className="servers">
              <label htmlFor="servers">
                <select onChange={onChange}>
                  {servers.map((server) => (
                    <option value={server.name} key={server.name}>
                      {server.name} -{" "}
                      {server.useTls ? "(secure)" : "(insecure)"}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </div>
        </Col>
      </div>
    );
  }
}
