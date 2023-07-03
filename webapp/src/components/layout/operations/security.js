import React from "react";
import Lock from "react-feather/dist/icons/lock";
import { Link } from "react-router-dom";
import { securityTitle } from "../security";

export const Security = ({ security, securitySchemes }) => {
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
