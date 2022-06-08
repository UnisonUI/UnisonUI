import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { setServerUrl, setVariables } from "../../features";

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
      const label = (
        <label htmlFor={selectId}>
          {name}
          {variable.description && ` (${variable.description})`}
        </label>
      );
      if (!variable.enum || variable.enum === 0) {
        return (
          <VariablesWrap key={selectId}>
            {label}
            <input
              type="text"
              id={selectId}
              value={variable.default}
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
            {label}
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
  return <div className="flex flex-col space-y-2">{variableComponents}</div>;
};

export default function Servers({ id, servers, type }) {
  const dispatch = useDispatch();
  const selectedServer = useSelector(
    (state) => state.request[id] && state.request[id].server.url
  );
  const computedUrl = useSelector(
    (state) => state.request[id] && state.request[id].server.computedUrl
  );

  const serverComponents = [];

  if (servers && type !== "asyncapi") {
    servers.forEach((server, idx) => {
      const componentId = `server-${idx}`;

      serverComponents.push(
        <div
          key={componentId}
          className="flex flex-row space-x-8 place-items-start"
        >
          <div className="flex flex-row space-x-8 place-items-center">
            <input
              type="radio"
              name="servers"
              id={componentId}
              value={server.url}
              checked={selectedServer === server.url}
              onChange={(event) =>
                dispatch(setServerUrl(id, event.target.value))
              }
            />
            <label htmlFor={componentId}>
              {server.url}
              {server.description && ` - ${server.description}`}
            </label>
          </div>
          {server.variables && (
            <Variables
              id={id}
              variables={server.variables}
              key={`${componentId}-variables`}
              component={componentId}
            />
          )}
        </div>
      );
    });
  }

  return (
    serverComponents.length > 0 && (
      <section className="section servers">
        <h1 className="title">Servers</h1>
        <div className="section-content">
          <div className="selections">{serverComponents}</div>
          <div className="selected">SELECTED: {computedUrl}</div>
        </div>
      </section>
    )
  );
}
