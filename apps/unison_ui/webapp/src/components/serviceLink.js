import React, { useEffect, useState } from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import ChevronRight from "react-feather/dist/icons/chevron-right";
import classNames from "classnames";
import {
  extractOpenApiOperations,
  extractAsyncAPIOperations,
  extractGrpcOperations,
} from "../utils";

const Title = ({ name, type }) => (
  <div className="title">
    <span className="name">{name}</span>
    <span className={classNames("type", type)}>{type}</span>
  </div>
);

const Operations = ({ service }) => {
  const location = useLocation();
  const isOpen = location.pathname === `/service/${service.id}`;
  const isOperation = (id) => isOpen && location.hash === `#${id}`;

  const operations = (function () {
    switch (service.type) {
      case "openapi":
        return extractOpenApiOperations(service.spec);

      case "asyncapi":
        return extractAsyncAPIOperations(service.spec);

      default:
        return extractGrpcOperations(service.spec);
    }
  })().map(({ id, name, isTag }, idx) => (
    <li
      key={`${service.id}-${id}-${idx}`}
      className={classNames("menu-item", { active: isOperation(id) })}
    >
      <Link
        to={`/service/${service.id}#${id}`}
        className={classNames("link", "leading-tight", {
          "text-xs": !isTag,
          "font-bold": isTag,
        })}
      >
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
          active: isNavLinkActive(service.id) && location.hash === "",
        })}
      >
        <NavLink to={`/service/${service.id}`} className="link">
          <Title
            name={getName(service)}
            type={service.type}
            className={(isActive) => classNames({ active: isActive })}
          />
          <ChevronRight
            className={classNames("chevron", {
              "rotate-90": isNavLinkActive(service.id),
            })}
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
