import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { server, setServerUrl, setVariables } from "../../features";

const VariablesWrap = ({ selectId, children }) => (
  <div className="flex flex-row space-x-4 place-items-center" key={selectId}>
    {children}{" "}
  </div>
);

const Variables = ({ id, variables, component }) => {
  const dispatch = useDispatch();
  const componentId = `${component}-variables`;
  const variableComponents = Object.entries(variables).map(
    ([name, variable]) => {
      const onChange = (event) => {
        const variables = {};
        variables[name] = event.target.value;
        dispatch(setVariables(id, variables));
      };
      const selectId = `${componentId}-${name}`;
      if (!variable.enum || variable.enum === 0) {
        return (
          <VariablesWrap key={selectId}>
            <label htmlFor={selectId}>{name}</label>
            <input
              type="text"
              id={selectId}
              defaultValue={variable.default}
              onChange={onChange}
            />
          </VariablesWrap>
        );
      } else {
        const items = [];
        variable.enum.forEach((value, idx) =>
          items.push(
            <option key={`${selectId}-${idx}`} value={value}>
              {value}
            </option>
          )
        );
        return (
          <VariablesWrap key={selectId}>
            <label htmlFor={selectId}>{name}</label>
            <select
              id={selectId}
              onChange={onChange}
              defaultValue={variable.default}
            >
              {items}
            </select>
          </VariablesWrap>
        );
      }
    }
  );
  return (
    <div className="border flex flex-col space-y-2">{variableComponents}</div>
  );
};

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
        serverComponents.push(
          <Variables
            id={id}
            variables={server.variables}
            key={`${componentId}-variables`}
            component={componentId}
          />
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
              event.target.type === "radio" &&
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
