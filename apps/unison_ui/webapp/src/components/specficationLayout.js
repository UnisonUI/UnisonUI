import React from "react";
import { useSelector } from "react-redux";
import { fetchSpec } from "../features/servicesSlice";
import Info from "./layout/info";

export default function SpecficationLayout({ id }) {
  const spec = useSelector((state) => fetchSpec(state, id));
  return (
    <section className="layout">
      <Info info={spec.info} />
    </section>
  );
}
