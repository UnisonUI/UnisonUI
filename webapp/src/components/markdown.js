import React from "react";
import { marked } from "marked";
import * as DOMPurify from "dompurify";
import { highlight } from "../utils";
import classNames from "classnames";

marked.setOptions({
  renderer: new marked.Renderer(),
  highlight: (str, lang) => highlight(str, lang),
});

export default function Markdown({ source, className }) {
  if (!source) return;
  const html = marked(source);
  const classes = ["markdown"];
  if (className) classes.push(className);
  return (
    <div
      className={classNames(classes)}
      dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(html) }}
    />
  );
}
