import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { setServer } from "../../features";

export default function Servers({ id, servers, type }) {
  const dispatch = useDispatch();
  const selectedServer = useSelector(
    (state) => state.request[id] && state.request[id].server
  );

  useEffect(() => {
    !selectedServer &&
      type !== "asyncapi" &&
      servers &&
      dispatch(setServer(id, servers[0].url));
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
    });
  }

  return (
    serverComponents.length > 0 && (
      <section className="servers">
        <h1 className="title">Servers</h1>
        <div
          className="selections"
          onChange={(event) => dispatch(setServer(id, event.target.value))}
        >
          {serverComponents}
        </div>
        <div className="selected">SELECTED: {selectedServer}</div>
      </section>
    )
  );
}
