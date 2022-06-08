import loadable from "@loadable/component";
import React, { useEffect, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useLocation } from "react-router-dom";
import Konami from "react-konami-code";
import Moon from "react-feather/dist/icons/moon";
import Sun from "react-feather/dist/icons/sun";

import { toast } from "react-toastify";

import * as cornify from "../cornified";
import classNames from "classnames";

import { handleEvent, selectAllServices } from "../features";

import ServiceLink from "./serviceLink";
import NoService from "./noService";

const SpecficationLayout = loadable(() =>
  import(/* webpackPrefetch: true */ "./specficationLayout")
);

export default function UnisonUILayout() {
  const dispatch = useDispatch();
  const services = useSelector(selectAllServices);
  const location = useLocation();

  const service = Object.values(services)
    .flat()
    .find((service) => location.pathname === `/service/${service.id}`);

  const [isDarkMode, setDarkMode] = useState(
    localStorage.theme === "dark" ||
      (!("theme" in localStorage) &&
        window.matchMedia("(prefers-color-scheme: dark)").matches)
  );

  function _toggleTheme() {
    const newIsDarkMode = !isDarkMode;
    if (newIsDarkMode) localStorage.theme = "dark";
    else localStorage.theme = "light";
    setDarkMode(newIsDarkMode);
  }

  function _connect() {
    const websocket = new WebSocket(
      `ws${window.location.protocol.replace("http", "")}//${
        window.location.host
      }/ws`
    );

    toast.loading("Connecting", {
      toastId: "loading",
      type: "info",
      autoClose: false,
    });
    let heartbeat;

    websocket.onopen = (_) => {
      toast.dismiss("loading");
      toast.dismiss("toastErrorId");

      heartbeat = setInterval(() => websocket.send("ping"), 30 * 1000);
    };
    websocket.onclose = (_) => {
      toast.warning("Connection lost", {
        toastId: "toastErrorId",
        autoClose: false,
      });

      setTimeout(() => {
        if (heartbeat) clearInterval(heartbeat);
        _connect();
      }, 1000);
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
    return <SpecficationLayout id={service.id} />;
  }

  return (
    <div
      id="outer-container"
      style={{ height: "100%" }}
      className={classNames("unisonui", { dark: isDarkMode })}
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
