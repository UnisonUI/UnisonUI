import React, { forwardRef, useEffect, useRef } from "react";
import Lock from "react-feather/dist/icons/lock";
import { operationsByTag } from "../../utils";
import Markdown from "../markdown";
import { Link, useLocation } from "react-router-dom";
import { securityTitle } from "./security";
import classNames from "classnames";

export const Operations = ({ service }) => {
  const location = useLocation();
  const operations = operationsByTag(service);
  const tagRefs = {};
  const operationRefs = {};

  Object.entries(operations).forEach(([tag, operations]) => {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    if (tag !== "") tagRefs[tag] = useRef();
    operations.forEach(({ method, path, operation }) => {
      const id = operation.operationId || `${method}-${path}`;
      // eslint-disable-next-line react-hooks/rules-of-hooks
      operationRefs[id] = useRef();
    });
  }, []);

  useEffect(() => {
    const hash = location.hash.substring(1);
    if (tagRefs[hash])
      tagRefs[hash].current.scrollIntoView({ behavior: "smooth" });
    else if (operationRefs[hash])
      operationRefs[hash].current.scrollIntoView({ behavior: "smooth" });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.hash]);

  const spec = service.spec;
  console.log(spec);
  const getTag = (tag) =>
    (spec.tags && spec.tags.find(({ name }) => name === tag)) || {
      name: tag,
    };

  return Object.entries(operations).map(([tag, operations]) => {
    return (
      <section key={tag} className="section operations border-top">
        {tag !== "" && <Tag tag={getTag(tag)} ref={tagRefs[tag]} />}
        {operations.map(({ method, path, operation }, idx) => {
          const refId = operation.operationId || `${method}-${path}`;
          return (
            <Operation
              key={`op-${idx}`}
              method={method}
              path={path}
              operation={operation}
              securitySchemes={spec.components.securitySchemes}
              ref={operationRefs[refId]}
            />
          );
        })}
      </section>
    );
  });
};

const Tag = forwardRef(({ tag }, ref) => (
  <section className="section tag" ref={ref}>
    <div className="title">{tag.name}</div>
    {tag.description && <Markdown source={tag.description} className="sm" />}
  </section>
));

const Operation = forwardRef(
  ({ method, path, operation, securitySchemes }, ref) => {
    const color = (() => {
      switch (method) {
        case "get":
          return "green";
        case "delete":
          return "red";
        case "post":
          return "blue";
        case "put":
          return "orange";
        default:
          return "purple";
      }
    })();
    return (
      <section className="section operation border-top" ref={ref}>
        <Security
          securitySchemes={securitySchemes}
          security={operation.security}
        />
        {operation.summary && <div className="title">{operation.summary}</div>}
        <div className="path">
          {path}{" "}
          <span className={classNames("badge", color, "plain", "uppercase")}>
            {method}
          </span>
        </div>
        {operation.description && (
          <Markdown source={operation.description} className="sm" />
        )}
      </section>
    );
  }
);
const Security = ({ security, securitySchemes }) => {
  if (Array.isArray(security) && security.length > 0) {
    const items = [];
    security.forEach((security) => {
      const [[name, options]] = Object.entries(security);
      const scheme = securitySchemes[name];
      items.push(
        <div className="tooltip">
          <Link to="#authentication" className="scheme">
            <span className="summary">{securityTitle(scheme, name)}</span>
          </Link>
          <div className="tooltip-text">
            <SecurityDescription
              name={name}
              scheme={scheme}
              options={options}
            />
          </div>
        </div>
      );
      items.push(<span className="mx-1">OR</span>);
    });
    items.pop();
    return (
      <div className="security">
        <Lock className="lock" />
        <div className="schemes">{items}</div>
      </div>
    );
  }
};

const SecurityDescription = ({ scheme, name, options }) => {
  switch (scheme.type) {
    case "oauth2":
      return (
        <div>
          <div>
            Need OAuth token <span className="text-blue-300">{name}</span> in{" "}
            <span className="font-bold">Authorization header</span>
          </div>
          <div>
            <span className="text-bold">Required scopes:</span>
            <br />
            <span className="ml-2">{options.join(" | ")}</span>
          </div>
        </div>
      );
    case "http":
      switch (scheme.scheme) {
        case "bearer":
          return (
            <div>
              Requires Bearer Token in{" "}
              <span className="font-bold">Authorization header</span>
            </div>
          );
        case "basic":
          return (
            <div>
              Requires Base 64 encoded username:password in{" "}
              <span className="font-bold">Authorization header</span>
            </div>
          );
        default:
          return (
            <div>
              <span className="font-bold">Authorization header</span>
            </div>
          );
      }
    case "apiKey":
      return (
        <div>
          Requires Token in <span className="font-bold">{scheme.name}</span>{" "}
          <span className="font-bold">{scheme.in}</span>
        </div>
      );
    default:
      return <div>{scheme.type}</div>;
  }
};
