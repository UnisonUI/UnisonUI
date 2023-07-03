import React, { Component } from "react";
import { Clear, Execute } from "./buttons";
import Request from "./request";
import { Response } from "./response";
import { stringify, Link, Collapse } from "./utils";
import axios from "axios";
const CancelToken = axios.CancelToken;

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
);

export default class Method extends Component {
  constructor(props) {
    super(props);
    this.state = {
      value: null,
      isVisible: false,
      tryItOutEnabled: false,
      executeInProgress: false,
      response: null,
      ws: null,
      cancel: CancelToken.source(),
    };
    this.toggleVisibility = this.toggleVisibility.bind(this);
    this.onTryoutClick = this.onTryoutClick.bind(this);
    this.onCancelClick = this.onCancelClick.bind(this);
    this.onExecute = this.onExecute.bind(this);
    this.onClear = this.onClear.bind(this);
    this.onUpdate = this.onUpdate.bind(this);
    this._request = this._request.bind(this);
    this._streaming = this._streaming.bind(this);
  }

  componentWillUnmount() {
    if (this.state.ws) this.state.ws.close();
  }

  onUpdate(value) {
    this.setState({ value });
  }

  onClear() {
    this.setState({ executeInProgress: false });
  }

  _request(id, service, method, server) {
    let data = {};
    try {
      data = JSON.parse(this.state.value);
    } catch (e) { }
    this.setState({ executeInProgress: true });
    axios
      .post(
        `/grpc/${id}/${service}/${method}`,
        { server, data },
        { cancelToken: this.state.cancel.token }
      )
      .then((response) => this.setState({ executeInProgress: false, response }))
      .catch((error) => {
        let response;
        if (axios.isCancel(error)) {
          response = null;
        } else {
          response = error.response;
        }
        this.setState({ executeInProgress: false, response });
      });
  }

  _streaming(id, service, method, server) {
    const ws = new WebSocket(
      `ws${location.protocol.replace("http", "")}//${location.host
      }/grpc/streaming/${id}/${service}/${method}?server=${server}`
    );
    ws.onopen = function (e) {
      ws.send(this.state.value);
      this.setState({ executeInProgress: true });
    }.bind(this);

    ws.onerror = function (e) {
      this.setState({
        executeInProgress: false,
        response: { status: 404, data: "not found" },
      });
    }.bind(this);

    ws.onmessage = function (e) {
      const data = JSON.parse(e.data);
      const response = this.state.response || [];
      if (data.error) {
        response.unshift({
          status: 400,
          data: data.error,
        });
      } else {
        response.unshift({
          status: 200,
          data: stringify(data.success),
        });
      }
      if (this.state.executeInProgress) {
        response.unshift({ value: this.state.value });
      }
      this.setState({ executeInProgress: false, response });
    }.bind(this);

    ws.onclose = function (e) {
      this.setState({ executeInProgress: false, ws: null });
    }.bind(this);

    this.setState({ ws });
  }

  onExecute() {
    let { id, method, service, server } = this.props;
    if (this.state.ws) {
      this.setState({ executeInProgress: true });
      this.state.ws.send(this.state.value);
    } else {
      id = btoa(id).replace("/", "_");
      server = server.name;
      if (method.streaming.server || method.streaming.client) {
        this._streaming(id, service, method.name, server);
      } else this._request(id, service, method.name, server);
    }
  }

  toggleVisibility() {
    this.setState({ isVisible: !this.state.isVisible });
  }

  onTryoutClick() {
    this.setState({ tryItOutEnabled: true });
  }

  onCancelClick() {
    if (this.state.ws) this.state.ws.close();
    else this.state.cancel.cancel();
    this.setState({ tryItOutEnabled: false });
  }

  render() {
    const methodName = "post";
    const { method, schema } = this.props;
    const { tryItOutEnabled, isVisible, executeInProgress, response, ws } =
      this.state;
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
                  Request (
                  <small>
                    {method.streaming.client && "stream "}
                    {method.inputType}
                  </small>
                  )
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
                !tryItOutEnabled || !ws || !response
                  ? "execute-wrapper"
                  : "btn-group"
              }
            >
              {!tryItOutEnabled ? null : <Execute onExecute={this.onExecute} />}

              {!tryItOutEnabled || !ws || !response ? null : (
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
                  Response (
                  <small>
                    {method.streaming.server && "stream "}
                    {method.outputType}
                  </small>
                  )
                </h4>
              </div>
              <div className="responses-inner">
                {response && <Response response={response} />}
              </div>
            </div>
          </div>
        </Collapse>
      </div>
    );
  }
}
