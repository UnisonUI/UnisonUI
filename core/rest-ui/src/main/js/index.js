require("./css/main.css");

import React from "react";
import ReactDOM from "react-dom";
import App from "./components/ui";

const wrapper = document.getElementById("app");
wrapper ? ReactDOM.render(<App />, wrapper) : false;
