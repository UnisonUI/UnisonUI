import React from "react";
import { useLocation } from "react-router-dom";
import AsyncApi from "@asyncapi/react-component";

function AsyncAPI() {
  const location = useLocation();
  const id = location.pathname.substring(1);
  const schema = { url: "/services/" + id };
  return <AsyncApi schema={schema} />;
}

export default AsyncAPI;
