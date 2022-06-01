import React from "react";
import { useLocation } from "react-router-dom";
import { useSelector } from "react-redux";
import AsyncApi from "@asyncapi/react-component";
import { fetchSpec } from "../features/services/servicesSlice";

export default function AsyncAPI() {
  const location = useLocation();
  const id = location.pathname.replace("/service/", "");
  const spec = useSelector((state) => fetchSpec(state, id));

  return <AsyncApi schema={spec} />;
}
