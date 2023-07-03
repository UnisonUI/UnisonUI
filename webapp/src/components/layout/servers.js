import React from "react";

export function Servers({ id, servers, type }) {
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
        </div>
      </section>
    )
  );
}

const VariablesWrap = ({ selectId, children }) => (
  <div className="flex flex-row space-x-4 place-items-center" key={selectId}>
    {children}{" "}
  </div>
);

const Variables = ({ id, variables, component }) => {
  const componentId = `${component}-variables`;
  const variableComponents = Object.entries(variables).map(
    ([name, variable]) => {
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
            <input type="text" id={selectId} value={variable.default} />
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
            <select id={selectId} defaultValue={variable.default}>
              {items}
            </select>
          </VariablesWrap>
        );
      }
    }
  );
  return <div className="flex flex-col space-y-2">{variableComponents}</div>;
};
