import React from "react";
import Markdown from "../markdown";

export default function Info({ info }) {
  return (
    info && (
      <section className="info">
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
