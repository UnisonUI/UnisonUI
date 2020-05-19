require("swagger-ui-react/swagger-ui.css");

import React, { Component } from "react";
import ReactDOM from "react-dom";
import {
  HashRouter as Router,
  Route,
  Switch,
  useParams
} from "react-router-dom";

import SwaggerUI from "swagger-ui-react";
import Endpoints from "./endpoints";

export default function App() {
  return (
     <Router>
      <nav className="menu">
        <Endpoints />
      </nav>
      <Switch>
        <Route path="/:name" children={<Swagger />} />
      </Switch>
      <Swagger />
    </Router>
  );
}

function Swagger() {
  let { name } = useParams();

  return (
    <main>
      {name ? (
        <SwaggerUI url={`/service/${name}`} docExpansion="list" />
      ) : (
        <div />
      )}
    </main>
  );
}
