import React from "react";
import { marked } from "marked";
import * as DOMPurify from "dompurify";
import { highlight } from "../utils";

marked.setOptions({
  renderer: new marked.Renderer(),
  highlight: (str, lang) => highlight(str, lang),
});

export default function Markdown({ source }) {
  const html = marked(source);
  return (
    <div
      className="markdown"
      dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(html) }}
    />
  );
}
