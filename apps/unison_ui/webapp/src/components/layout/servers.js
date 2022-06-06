import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { server, setServerUrl, setVariables } from "../../features";

export default function Servers({ id, servers, type }) {
  const dispatch = useDispatch();
  const selectedServer = useSelector(
    (state) => state.request[id] && state.request[id].server.url
  );
  const computedUrl = useSelector(
    (state) => state.request[id] && state.request[id].server.computedUrl
  );

  useEffect(() => {
    if (!selectedServer && type !== "asyncapi" && servers) {
      const variables = {};
      servers[0].variables &&
        Object.entries(servers[0].variables).forEach(([name, variable]) => {
          variables[name] = variable.default;
        });
      dispatch(server({ id, server: { url: servers[0].url, variables } }));
    }
  });

  const serverComponents = [];

  if (servers && type !== "asyncapi") {
    servers.forEach((server, idx) => {
      const componentId = `server-${idx}`;

      serverComponents.push(
        <div key={componentId}>
          <input
            type="radio"
            name="servers"
            id={componentId}
            value={server.url}
            defaultChecked={
              (selectedServer && selectedServer === server.url) || idx === 0
            }
          />
          <label htmlFor={componentId}>
            {server.url}
            {server.description && ` - ${server.description}`}
          </label>
        </div>
      );
      if (server.variables) {
        const componentId = `server-${idx}-variables`;
        const variables = Object.entries(server.variables).map(
          ([name, variable]) => {
            const selectId = `${componentId}-${name}`;
            const items = [];
            if (!variable.enum || variable.enum === 0)
              items.push(
                <option key={`${selectId}-0`} value={variable.default}>
                  {variable.default}
                </option>
              );
            else
              variable.enum.forEach((value, idx) =>
                items.push(
                  <option key={`${selectId}-${idx}`} value={value}>
                    {value}
                  </option>
                )
              );
            return (
              <select
                onChange={(event) => {
                  const variables = {};
                  variables[name] = event.target.value;
                  dispatch(setVariables(id, variables));
                }}
                key={id}
                defaultValue={variable.default}
              >
                {items}
              </select>
            );
          }
        );
        serverComponents.push(
          <div className="border flex flex-row" key="componentId">
            {variables}
          </div>
        );
      }
    });
  }

  return (
    serverComponents.length > 0 && (
      <section className="section servers">
        <h1 className="title">Servers</h1>
        <div className="section-content">
          <div
            className="selections"
            onChange={(event) =>
              event.target.tagName === "INPUT" &&
              dispatch(setServerUrl(id, event.target.value))
            }
          >
            {serverComponents}
          </div>
          <div className="selected">SELECTED: {computedUrl}</div>
        </div>
      </section>
    )
  );
}
