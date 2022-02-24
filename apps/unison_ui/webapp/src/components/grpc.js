import React, { Component } from "react";
import { useLocation } from "react-router-dom";
import axios from "axios";
import Error from "./grpc/error";
import Layout from "./grpc/layout";

class Grpc extends Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      spec: null,
    };
    this.location = useLocation();
    this._loadSpec = this._loadSpec.bind(this);
  }

  _loadSpec(id) {
    if (id) {
      axios
        .get(`/services/${id}`)
        .then((res) => this.setState({ spec: res.data }))
        .catch((error) => this.setState({ error: error.response.data }));
    }
  }

  componentDidMount() {
    this._loadSpec(this.location.pathname.substring(1));
  }

  componentDidUpdate(prevProps) {
    if (this.location.pathname !== prevlocation.pathname) {
      this._loadSpec(this.location.pathname.substring(1));
    }
  }

  render() {
    const loading = this.loadingWidget();
    const id = this.location.pathname.substring(1);
    return (
      <div>
        {!loading ? (
          <Layout title={this.props.title} spec={this.state.spec} id={id} />
        ) : (
          loading
        )}
      </div>
    );
  }

  loadingWidget() {
    let loadingMessage = null;
    if (this.state.spec) return loadingMessage;
    if (!this.state.error) {
      loadingMessage = (
        <div className="info">
          <div className="loading-container">
            <div className="loading" />
          </div>
        </div>
      );
    } else {
      loadingMessage = (
        <div className="info">
          <div className="loading-container">
            <h4 className="title">Failed to load gRPC definition.</h4>
            <Error message={this.state.error} />
          </div>
        </div>
      );
    }
    return (
      <div className="swagger-ui">
        <div className="loading-container">{loadingMessage}</div>
      </div>
    );
  }
}

export default Grpc;
