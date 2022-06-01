import loadable from "@loadable/component";
import { Moon, Sun } from "react-feather";
import React, { useEffect, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useLocation } from "react-router-dom";
import ServiceLink from "./serviceLink";
import * as cornify from "../cornified";
import Konami from "react-konami-code";
import NoService from "./noService";
import {
  handleEvent,
  selectAllServices,
} from "../features/services/servicesSlice";
const AsyncAPI = loadable(() => import("./asyncapi"));
const OpenAPI = loadable(() => import("./openapi"));
const GRPC = loadable(() => import("./grpc"));

export default function UnisonUILayout() {
  const dispatch = useDispatch();
  const services = useSelector(selectAllServices);
  const location = useLocation();
  const service = Object.values(services)
    .flat()
    .find((service) => location.pathname === `/service/${service.id}`);

  const [isDarkMode, setDarkMode] = useState(
    localStorage.getItem("darkMode") === "true"
  );

  function _toggleTheme() {
    const newTheme = !isDarkMode;
    localStorage.setItem("darkMode", newTheme);
    setDarkMode(newTheme);
  }

  function _connect() {
    const websocket = new WebSocket(
      `ws${window.location.protocol.replace("http", "")}//${
        window.location.host
      }/ws`
    );

    websocket.onclose = (_) => {
      setTimeout(() => _connect(), 1000);
    };

    websocket.onmessage = (e) => {
      if (e.data) {
        handleMessage(JSON.parse(e.data));
      }
    };
  }

  function handleMessage(data) {
    if (data.event) dispatch(handleEvent(data));
    else data.events.forEach((data) => dispatch(handleEvent(data)));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => _connect(), []);

  function getServices() {
    const items = [];
    if (Object.keys(services).length) {
      const entries = Object.entries(services);
      entries.sort((a, b) => a[0].localeCompare(b[0]));
      entries.forEach(([name, services]) => {
        items.push(<ServiceLink services={services} key={name} />);
      });
    } else {
      items.push(
        <h1 key="0" style={{ padding: "0.5em" }}>
          No services available
        </h1>
      );
    }
    return <ul>{items}</ul>;
  }

  function _loadComponent(service) {
    switch (service.type) {
      case "openapi":
        return <OpenAPI useProxy={service.useProxy} />;
      case "asyncapi":
        return <AsyncAPI />;
      case "grpc":
        return <GRPC title={service.name} />;
      default:
        break;
    }
  }

  return (
    <div
      id="outer-container"
      style={{ height: "100%" }}
      className={isDarkMode ? "dark" : ""}
    >
      <Konami
        action={() => cornify.pizzazz()}
        timeout={15000}
        onTimeout={() => cornify.clear()}
      />
      <header className="header">
        <div className="logo">UnisonUI</div>
        <button className="themeSwitch" onClick={_toggleTheme}>
          {isDarkMode ? <Sun size={42} /> : <Moon size={42} />}
        </button>
      </header>
      <main id="page-wrap">
        <nav className="menu">{getServices()}</nav>
        <section className="content">
          {service ? _loadComponent(service) : <NoService />}
        </section>
      </main>
    </div>
  );
}
