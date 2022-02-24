import React from "react";
import ReactDOM from "react-dom";
import App from "./components/ui";

import "./css/main.scss";
import "./css/swagger-ui.scss";
import "@asyncapi/react-component/styles/default.css";

const wrapper = document.getElementById("app");
if (wrapper) ReactDOM.render(<App />, wrapper);
