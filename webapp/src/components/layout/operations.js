import React, { forwardRef, useEffect, useRef } from "react";
import { operationsByTag } from "../../utils";
import Markdown from "../markdown";
import { useLocation } from "react-router-dom";
import classNames from "classnames";
import { Request } from "./operations/request";
import { Security } from "./operations/security";
import { Parameters } from "./operations/parameters";

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

const operationColor = (method) => {
  switch (method) {
    case "get":
      return "green";
    case "delete":
      return "red";
    case "post":
      return "blue";
    case "put":
      return "orange";
    case "patch":
      return "yellow";
    default:
      return "purple";
  }
};

const Operation = forwardRef(
  ({ method, path, operation, securitySchemes }, ref) => {
    return (
      <section
        className={classNames(
          "section",
          "operation",
          operationColor(method),
          "border-top",
          { deprecated: operation.deprecated }
        )}
        ref={ref}
      >
        <div
          className={classNames("flex", {
            "justify-between": operation.deprecated,
            "justify-end": !operation.deprecated,
          })}
        >
          {operation.deprecated && (
            <div className="text-red-500">DEPRECATED</div>
          )}
          <Security
            securitySchemes={securitySchemes}
            security={operation.security}
          />
        </div>
        {operation.summary && <div className="title">{operation.summary}</div>}
        <div className="path">
          <span className="method"> {method} </span> {path}
        </div>
        {operation.description && (
          <Markdown source={operation.description} className="sm" />
        )}
        <div className="request">
          <h1>REQUEST</h1>
          <Parameters parameters={operation.parameters} />
          <Request body={operation.requestBody} />
        </div>
      </section>
    );
  }
);
