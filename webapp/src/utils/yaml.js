import { JSON_SCHEMA, types, load } from "js-yaml";

const DEFAULT_SCHEMA_WITHOUT_TIMESTAMP = JSON_SCHEMA.extend({
  implicit: [types.merge],
  explicit: [types.binary, types.omap, types.pairs, types.set],
});

export const parseYaml = (str, opts) =>
  load(str, { schema: DEFAULT_SCHEMA_WITHOUT_TIMESTAMP, ...opts });
