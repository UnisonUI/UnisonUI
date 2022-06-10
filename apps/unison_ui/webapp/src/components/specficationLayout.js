import React from "react";
import { useSelector } from "react-redux";
import { fetchService } from "../features";
import { Authentication, Info, Servers } from "./layout";

export default function SpecficationLayout({ id }) {
  const service = useSelector((state) => fetchService(state, id));
  const spec = service.spec;

  return (
    <section className="layout">
      <Info info={spec.info} />
      <Servers id={id} servers={spec.servers} type={service.type} />
      <Authentication
        authentication={spec.components && spec.components.securitySchemes}
      />
    </section>
  );
}
