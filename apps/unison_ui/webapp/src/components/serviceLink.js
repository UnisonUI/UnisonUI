import React from "react";
import { NavLink, useLocation } from "react-router-dom";
import classNames from "classnames";

const Title = ({ name, type }) => (
  <div className="title">
    <span className="name">{name}</span>
    <span className={classNames("type", type)}>{type}</span>
  </div>
);

const Operations = ({ service }) => {
  const operations = (function () {
    switch (service.type) {
      case "openapi":
        return Object.entries(service.spec.paths).flatMap(([pathName, path]) =>
          Object.entries(path).map(
            ([method, path]) =>
              path.summary ||
              path.description ||
              `${pathName} - ${method.toUpperCase()}`
          )
        );

      case "asyncapi":
        return Object.entries(service.spec.channels).map(
          ([channelName, channel]) =>
            channel.summary || channel.description || channelName
        );

      default:
        return [];
    }
  })();

  return operations.map((operation) => (
    <li key={`${service.id}-${operation}`} className="menu-item">
      {operation}
    </li>
  ));
};

const getName = (service) => {
  switch (service.type) {
    case "openapi":
      return service.spec.info.title;
    case "asyncapi":
      return service.spec.info.title;
    default:
      return service.name;
  }
};

export default function ServiceLink({ services }) {
  const location = useLocation();
  const isNavLinkActive = (id) => location.pathname === `/service/${id}`;

  const items = [];

  [...services].sort((a, b) => a.name.localeCompare(b.name));
  services.forEach((service) => {
    items.push(
      <li
        key={service.id}
        className={classNames("menu-item", {
          active: isNavLinkActive(service.id),
        })}
      >
        <NavLink to={`/service/${service.id}`}>
          <Title
            name={getName(service)}
            type={service.type}
            className={(isActive) => classNames({ active: isActive })}
          />
        </NavLink>
      </li>
    );
    items.push(
      <Operations service={service} key={`operations-${service.id}`} />
    );
  });
  return items;
}
