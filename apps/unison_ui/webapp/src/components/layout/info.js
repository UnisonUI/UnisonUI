import loadable from "@loadable/component";
import React from "react";
const Markdown = loadable(() =>
  import(/* webpackPrefetch: true */ "../markdown")
);

export function Info({ info }) {
  return (
    info && (
      <section className="info">
        <Logo logo={info["x-logo"]} contact={info.contact} />
        <section className="description">
          <h1 className="title">
            {info.title} <span>({info.version})</span>
          </h1>
          <LicenseAndContact
            license={info.license}
            contact={info.contact}
            termsOfService={info.termsOfService}
          />
          <Description text={info.description} />
        </section>
      </section>
    )
  );
}

function LicenseAndContact({ license, contact, termsOfService }) {
  const items = [];
  if (license)
    items.push(
      <li key="info-license">
        {license.url ? (
          <a
            href={license.url}
            className="badge orange"
            target="_blank"
            rel="nofollow noopener noreferrer"
          >
            {license.name}
          </a>
        ) : (
          <span className="badge orange">{license.name}</span>
        )}
      </li>
    );
  if (contact) {
    if (contact.url)
      items.push(
        <li key="info-contact-url">
          <a
            href={contact.url}
            className="badge purple"
            target="_blank"
            rel="nofollow noopener noreferrer"
          >
            {contact.name || "Support"}
          </a>
        </li>
      );
    if (contact.email)
      items.push(
        <li key="info-contact-email">
          <a
            href={`mailto:${contact.email}`}
            className="badge purple"
            target="_blank"
            rel="nofollow noopener noreferrer"
          >
            {contact.email}
          </a>
        </li>
      );
  }
  if (termsOfService)
    items.push(
      <li key="info-termsOfServicel">
        <a
          href={termsOfService}
          className="badge purple"
          target="_blank"
          rel="nofollow noopener noreferrer"
        >
          Terms of service
        </a>
      </li>
    );
  return items.length > 0 && <ul className="license-contact">{items}</ul>;
}

function Description({ text }) {
  return (
    <section className="mt-4">
      <Markdown source={text} />
    </section>
  );
}

function Logo({ logo, contact }) {
  if (logo) {
    const img = <img src={logo.url} alt={logo.altText} />;
    const compoment = contact ? (
      <a href={contact.url} target="_blank" rel="nofollow noopener noreferrer">
        {img}
      </a>
    ) : (
      img
    );
    return <div className="logo">{compoment}</div>;
  }
}
