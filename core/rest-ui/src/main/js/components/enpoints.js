import React, { Component } from "react";
import ReactDOM from "react-dom";
import { Link } from "react-router-dom";
import axios from "axios";

export default class Endpoints extends Component {
  state = {
    endpoints: []
  };
  eventSource = new EventSource("/events");

  componentDidMount() {
    axios.get(`/endpoints`).then(res => {
      const endpoints = res.data.map(event => {
        return { serviceName: event.serviceName };
      });
      endpoints.sort((a, b) => b.serviceName.localeCompare(a.serviceName));
      this.setState({ endpoints });
    });
    this.eventSource.onmessage = e => {
      if (e.data) {
        this.handleEndpoint(JSON.parse(e.data));
      }
    };
  }
  handleEndpoint(data) {
    let endpoints;
    if (data.event == "up") {
      endpoints = this.state.endpoints;
      if (!endpoints.find(item => item.serviceName == data.serviceName)) {
        endpoints.push({ serviceName: data.serviceName });
        endpoints.sort((a, b) => b.serviceName.localeCompare(a.serviceName));
      }
    } else {
      endpoints = this.state.endpoints.filter(
        item => item.serviceName != data.serviceName
      );
    }
    this.setState({ endpoints });
  }
  render() {
    if (this.state.endpoints.length) {
      return (
        <section>
          <h1>List of services</h1>
          <ul className="items">
            {this.state.endpoints.map(endpoint => {
              return (
                <li key={endpoint}>
                  <Link to={`/?name=${endpoint.serviceName}`}>
                    {endpoint.serviceName}
                  </Link>
                </li>
              );
            })}
          </ul>
        </section>
      );
    } else {
      return <h1>No service available</h1>;
    }
  }
}
