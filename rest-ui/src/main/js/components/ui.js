require("swagger-ui-react/swagger-ui.css");

import PropTypes from "prop-types";
import React, { Component } from "react";
import ReactDOM from "react-dom";
import {
  HashRouter as Router,
  Route,
  Switch,
  withRouter,
  useParams
} from "react-router-dom";

import SwaggerUI from "swagger-ui-react";
import Services from "./services";

export default function App() {
  return (
    <Router>
      <nav className="menu">
        <Services />
      </nav>
      <SwaggerWithRouter />
    </Router>
  );
}

class Swagger extends Component {
  static propTypes = {
    match: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired
  };

  render() {
    let name = this.props.location.pathname.substring(1);
    return (
      <main>
        {name ? (
          <SwaggerUI url={`/services/${name}`} docExpansion="list" />
        ) : (
          <div />
        )}
      </main>
    );
  }
}

const SwaggerWithRouter = withRouter(Swagger);
