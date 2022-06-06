import "core-js/es/array";
import "core-js/proposals/array-grouping";

import React from "react";
import { Provider } from "react-redux";
import { createRoot } from "react-dom/client";
import { BrowserRouter as Router } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import UnisonUILayout from "./components/ui";
import store from "./store";

import "./css/main.scss";
import "prismjs/themes/prism-dark.css";
import "react-toastify/dist/ReactToastify.css";

const container = document.getElementById("app");
const root = createRoot(container);
root.render(
  <Provider store={store}>
    <Router>
      <UnisonUILayout />
    </Router>
    <ToastContainer theme="colored" position="bottom-left" />
  </Provider>
);
