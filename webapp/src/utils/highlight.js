import * as Prism from "prismjs";
import "prismjs/components/prism-bash.js";
import "prismjs/components/prism-http.js";
import "prismjs/components/prism-markup-templating.js"; // dep of php
import "prismjs/components/prism-markup.js"; // xml
import "prismjs/components/prism-javascript.js";
import "prismjs/components/prism-yaml.js";

const DEFAULT_LANG = "js";

Prism.languages.insertBefore(
  "javascript",
  "string",
  {
    "property string": {
      pattern: /([{,]\s*)"(?:\\.|[^\\"\r\n])*"(?=\s*:)/i,
      lookbehind: true,
    },
  },
  undefined
);

Prism.languages.insertBefore(
  "javascript",
  "punctuation",
  {
    property: {
      pattern: /([{,]\s*)[a-z]\w*(?=\s*:)/i,
      lookbehind: true,
    },
  },
  undefined
);

function mapLang(lang) {
  return (
    {
      json: "js",
      "c++": "cpp",
      "c#": "csharp",
      "objective-c": "objectivec",
      shell: "bash",
      viml: "vim",
    }[lang] || DEFAULT_LANG
  );
}

export function highlight(source, lang = DEFAULT_LANG) {
  lang = lang.toLowerCase();
  let grammar = Prism.languages[lang];
  if (!grammar) {
    grammar = Prism.languages[mapLang(lang)];
  }
  return Prism.highlight(source, grammar, lang);
}
