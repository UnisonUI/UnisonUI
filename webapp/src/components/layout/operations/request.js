import React from "react";

export const Request = ({ body }) => {
  if (!body) return;

  return <div>{JSON.stringify(body)}</div>;
};
