import React, { useEffect, useRef } from "react";
import { useSelector } from "react-redux";
import { useLocation } from "react-router-dom";
import { fetchService } from "../features";
import { resolveRef } from "../utils";
import { Authentication, Info, Operations, Servers } from "./layout";

export default function SpecficationLayout({ id }) {
  const authenticationRef = useRef();
  const layoutRef = useRef();
  const location = useLocation();
  const service = useSelector((state) => fetchService(state, id));

  useEffect(() => {
    switch (location.hash) {
      case "":
        layoutRef.current.scrollIntoView({ behavior: "smooth" });
        break;
      case "#authentication":
        authenticationRef.current.scrollIntoView({ behavior: "smooth" });
        break;
    }
  });
  const spec = service.spec;
  console.log(spec);
  const a = resolveRef(spec);
  console.log(a);
  return (
    <section className="layout" ref={layoutRef}>
      <Info info={spec.info} />
      <Servers id={id} servers={spec.servers} type={service.type} />
      <Authentication
        ref={authenticationRef}
        authentication={spec.components && spec.components.securitySchemes}
      />
      <Operations service={service} />
    </section>
  );
}
