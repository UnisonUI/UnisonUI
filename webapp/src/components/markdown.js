import React from "react";
import { marked } from "marked";
import { gfmHeadingId } from "marked-gfm-heading-id";
import { markedHighlight } from "marked-highlight";
import { mangle } from "marked-mangle";
import * as DOMPurify from "dompurify";
import { highlight } from "../utils";
import classNames from "classnames";

marked
  .setOptions({
    renderer: new marked.Renderer(),
  })
  .use(mangle(), gfmHeadingId(), markedHighlight({ highlight }));

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
