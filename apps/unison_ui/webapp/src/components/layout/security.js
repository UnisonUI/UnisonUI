import capitalize from "lodash-es/capitalize";
import React, { forwardRef } from "react";
import Markdown from "../markdown";

export const Authentication = forwardRef(({ authentication }, ref) => {
  return (
    authentication && (
      <section className="section authentication" ref={ref}>
        <h1 className="title">Authentication</h1>
        <table className="section-content">
          {Object.entries(authentication).map(([name, security]) => (
            <Security
              name={name}
              security={security}
              key={`security-${name}`}
            />
          ))}
        </table>
      </section>
    )
  );
});
export const securityTitle = (security, name) => {
  const type = (() => {
    switch (security.type) {
      case "oauth2":
        return "OAuth";
      case "apiKey":
        return "Api Key";
      case "http":
        return "Http";
      case "openIdConnect":
        return "Http";
      default:
        return type;
    }
  })();
  let title = `${type} (${name})`;
  if (type === "Http") title = `Http ${capitalize(security.scheme)}`;
  return title;
};

const Security = ({ name, security }) => {
  const component = (() => {
    switch (security.type) {
      case "oauth2":
        return <Oauth2 security={security} />;
      case "apiKey":
        return <ApiKey security={security} />;
      case "http":
        return <Http security={security} />;
      case "openIdConnect":
        return <OpenIdConnect security={security} />;
      default:
        return null;
    }
  })();
  return (
    <div>
      <h1>{securityTitle(security, name)}</h1>
      {component}
      <Markdown source={security.description} className="sm" />
    </div>
  );
};
const Http = ({ security }) => {
  switch (security.scheme) {
    case "bearer":
      return <HttpBearer />;
    case "basic":
      return <HttpBasic />;
  }
};

const HttpBasic = () => (
  <div>
    <div>
      Send <span className="font-bold">Authorization</span> in{" "}
      <span className="font-bold">header</span> containing the word{" "}
      <span className="font-bold">Bearer</span> followed by a space and a base64
      encoded string of <span className="font-bold">username:password</span>.
    </div>
    <div>
      <input type="text" placeholder="username" />{" "}
      <input type="text" placeholder="password" />{" "}
      <button className="badge blue">SET</button>
    </div>
  </div>
);

const HttpBearer = () => (
  <div>
    <div>
      Send <span className="font-bold">Authorization</span> in{" "}
      <span className="font-bold">header</span> containing the word{" "}
      <span className="font-bold">Bearer</span> followed by a space and a Token
      String.
    </div>
    <div>
      <input type="text" placeholder="api-token" />{" "}
      <button className="badge blue">SET</button>
    </div>
  </div>
);

const OpenIdConnect = ({ security }) => (
  <div>
    <div>
      Send <span className="font-bold">{security.name}</span> in{" "}
      <span className="font-bold">{security.in}</span>
    </div>
    <div>
      <input type="text" placeholder="api-token" />{" "}
      <button className="badge blue">SET</button>
    </div>
  </div>
);

const ApiKey = ({ security }) => (
  <div>
    <div>
      Send <span className="font-bold">{security.name}</span> in{" "}
      <span className="font-bold">{security.in}</span>
    </div>
    <div>
      <input type="text" placeholder="api-token" />{" "}
      <button className="badge blue">SET</button>
    </div>
  </div>
);

const Oauth2 = ({ security }) =>
  Object.entries(security.flows).map(([type, flow]) => (
    <Flow key={`flow-${type}`} type={type} flow={flow} />
  ));

const Flow = ({ type, flow }) => {
  const typeName = (() => {
    switch (type) {
      case "authorizationCode":
        return "authorization code";
      case "clientCredentials":
        return "client credentials";
      default:
        return type;
    }
  })();
  return (
    <div>
      <h1 className="uppercase">{typeName} flow</h1>
      <Url url={flow.authorizationUrl} name="Auth" />
      <Url url={flow.tokenUrl} name="Token" />
      <Url url={flow.tokenUrl} name="Token" />
      <Scopes scopes={flow.scopes} />
    </div>
  );
};

const Url = ({ url, name }) =>
  url && (
    <div className="flex space-x-4">
      <div>{name} URL</div>
      <div>{url}</div>
    </div>
  );

const Scopes = ({ scopes }) => (
  <div className="scopes">
    <h1>Scopes</h1>
    {Object.entries(scopes).map(([scope, name]) => (
      <label className="scope" key={scope} htmlFor={scope}>
        <input type="checkbox" role="switch" id={scope} />
        <span>{name}</span>
      </label>
    ))}
  </div>
);
