import loadable from "@loadable/component";
import React, { forwardRef } from "react";
const Markdown = loadable(() =>
  import(/* webpackPrefetch: true */ "../markdown")
);

export const Authentication = forwardRef(({ authentication }, ref) => {
  return (
    authentication && (
      <section className="section authentication" ref={ref}>
        <h1 className="title">Authentication</h1>
        <div className="section-content">
          {Object.entries(authentication).map(([name, security]) => (
            <Security
              name={name}
              security={security}
              key={`security-${name}`}
            />
          ))}
        </div>
      </section>
    )
  );
});

function Security({ name, security }) {
  const { component, type } = (() => {
    switch (security.type) {
      case "oauth2":
        return { component: <Oauth2 security={security} />, type: "OAuth" };
      default:
        return { component: null, type: security.type };
    }
  })();
  return (
    <div>
      <h1>
        {type} ({name})
      </h1>
      {component}
    </div>
  );
}

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
