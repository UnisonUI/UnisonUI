import React from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import classNames from "classnames";
import { capitalize } from "lodash";
import { extractOpenApiOperations } from "../utils/openapi";
import { ChevronDown, ChevronRight } from "react-feather";
const Title = ({ name, type }) => (
  <div className="title">
    <span className="name">{name}</span>
    <span className={classNames("type", type)}>{type}</span>
  </div>
);

const Operations = ({ service }) => {
  const location = useLocation();
  const isOpen = location.pathname === `/service/${service.id}`;
  const isOperation = (id) =>
    location.pathname === `/service/${service.id}` &&
    location.hash === `#${id}`;

  const operations = (function () {
    switch (service.type) {
      case "openapi": {
        const operations = extractOpenApiOperations(service.spec);
        return Object.entries(operations).flatMap(([tag, operations]) => {
          const result = [];
          if (tag !== "") result.push(capitalize(tag));
          operations.forEach(({ name, id }) => result.push({ id, name }));
          return result;
        });
      }
      case "asyncapi":
        return Object.entries(service.spec.channels).flatMap(
          ([channelName, channel]) => {
            const compute = (channel, type) => {
              const name =
                channel.summary ||
                channel.description ||
                `${type} - ${channelName}`;
              return {
                name,
                id: channel.operationId || `${type}-${channelName}`,
              };
            };
            const result = [];
            if (channel.subscribe)
              result.push(compute(channel.subscribe, "subscribe"));
            if (channel.publish)
              result.push(compute(channel.publish, "publish"));
            return result;
          }
        );

      default:
        return [];
    }
  })().map(({ id, name }) => (
    <li key={`${service.id}-${id}`} className="menu-item">
      <Link to={`/service/${service.id}#${id}`} className="link">
        <div className={classNames({ active: isOperation(id) })}>{name}</div>
      </Link>
    </li>
  ));
  return (
    <li className={classNames({ closed: !isOpen })}>
      <ul>{operations}</ul>
    </li>
  );
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
        <NavLink to={`/service/${service.id}`} className="link">
          <Title
            name={getName(service)}
            type={service.type}
            className={(isActive) => classNames({ active: isActive })}
          />
          {isNavLinkActive(service.id) ? (
            <ChevronDown className="chevron" />
          ) : (
            <ChevronRight className="chevron" />
          )}
        </NavLink>
      </li>
    );
    items.push(
      <Operations service={service} key={`operations-${service.id}`} />
    );
  });
  return items;
}
