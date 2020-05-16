import "swagger-ui-react/swagger-ui.css";

import React, { Component } from "react";
import ReactDOM from "react-dom";
import { BrowserRouter as Router, useLocation } from "react-router-dom";

import SwaggerUI from "swagger-ui-react";
import Endpoints from "./enpoints";

export default function App() {
  return (
    <Router>
      <Route />
    </Router>
  );
}
function useQuery() {
  return new URLSearchParams(useLocation().search);
}

function Route() {
  return (
    <Router>
      <nav className="menu">
        <Endpoints />
      </nav>
      <Swagger />
    </Router>
  );
}

function Swagger() {
  let url = useQuery().get("name");
  return (
    <main>{url ? <SwaggerUI url={`/service/${url}`} docExpansion="list" /> : <div />}</main>
  );
}
