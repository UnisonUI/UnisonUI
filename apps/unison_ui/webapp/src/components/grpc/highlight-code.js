import { Light as SyntaxHighlighter } from "react-syntax-highlighter";
import json from "react-syntax-highlighter/dist/esm/languages/hljs/json";
import xml from "react-syntax-highlighter/dist/esm/languages/hljs/xml";
import obsidian from "react-syntax-highlighter/dist/esm/styles/hljs/obsidian";
import React from "react";
import { stringify } from "./utils";
SyntaxHighlighter.registerLanguage("json", json);
SyntaxHighlighter.registerLanguage("xml", xml);

export const HighlightCode = ({ className, code }) => (
  <div className="highlight-code">
    <SyntaxHighlighter className={className + " microlight"} style={obsidian}>
      {stringify(code)}
    </SyntaxHighlighter>
  </div>
);
