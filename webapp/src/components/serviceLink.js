import React, { useState } from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import ChevronRight from "react-feather/dist/icons/chevron-right";
import classNames from "classnames";
import { extractOperations } from "../utils";

const Title = ({ name, type }) => (
  <div className="title">
    <span className="name">{name}</span>
    <span className={classNames("type", type)}>{type}</span>
  </div>
);

const Operations = ({ service, isOpen }) => {
  const location = useLocation();
  const isOperation = (id) => isOpen && location.hash === `#${id}`;

  const operations = extractOperations(service).map(
    ({ id, name, isTag, deprecated }, idx) => (
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
          <div
            className={classNames({
              active: isOperation(id),
              "opacity-60": deprecated,
            })}
          >
            {name}
          </div>
        </Link>
      </li>
    )
  );

  return (
    <li className={classNames("menu-operation", { closed: !isOpen })}>
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

export const ServiceLink = ({ services }) =>
  services.map((service) => <Menu key={service.id} service={service} />);

const Menu = ({ service }) => {
  const location = useLocation();
  const [isOpen, setOpen] = useState(
    location.pathname === `/service/${service.id}`
  );
  const toggleMenu = (_) => setOpen(!isOpen);
  const title = <Title name={getName(service)} type={service.type} />;

  const chevron = (
    <ChevronRight
      className={classNames("chevron", {
        "rotate-90": isOpen,
      })}
    />
  );
  return (
    <section className={classNames({ open: isOpen }, "menu-links")}>
      <ul>
        <li
          className={classNames("menu-item", {
            active:
              location.pathname === `/service/${service.id}` &&
              location.hash === "",
          })}
          onClick={toggleMenu}
        >
          {!isOpen ? (
            <NavLink to={`/service/${service.id}`} className="link">
              {title}
              {chevron}
            </NavLink>
          ) : (
            <div className="link">
              {title}
              {chevron}
            </div>
          )}
        </li>
        <Operations service={service} isOpen={isOpen} />
      </ul>
    </section>
  );
};
export const ServiceOption = ({ services }) =>
  services.map((service) => (
    <option key={`option-${service.id}`} value={`/service/${service.id}`}>
      {getName(service)}
    </option>
  ));
