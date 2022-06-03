import React from "react";
import { useSelector } from "react-redux";
import AsyncApi from "@asyncapi/react-component";
import { fetchSpec } from "../features/services/servicesSlice";

export default function AsyncAPI({ id }) {
  const spec = useSelector((state) => fetchSpec(state, id));

  return <AsyncApi schema={spec} />;
}
