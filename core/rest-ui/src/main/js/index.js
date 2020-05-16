require("./css/main.css");
import "core-js";
import React from "react";
import ReactDOM from "react-dom";
import App from "./components/ui";

const wrapper = document.getElementById("app");
wrapper ? ReactDOM.render(<App />, wrapper) : false;
