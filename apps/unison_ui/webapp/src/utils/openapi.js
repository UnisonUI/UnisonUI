import { bundle } from "@redocly/openapi-core/lib/bundle";
// eslint-disable-next-line import/no-internal-modules
import { Config } from "@redocly/openapi-core/lib/config/config";
/* tslint:disable-next-line:no-implicit-dependencies */
import { convertObj } from "swagger2openapi";
import { parseYaml } from "./yaml";
import capitalize from "lodash-es/capitalize";

export async function parseOpenApi(input) {
  const config = new Config({});
  const specUrlOrObject = parseYaml(input, { filename: "" });
  const bundleOpts = {
    config,
    base: window.location.href,
    resolve: { http: { customFetch: global.fetch } },
    doc: {
      source: { absoluteRef: "" },
      parsed: specUrlOrObject,
    },
  };

  const {
    bundle: { parsed },
  } = await bundle(bundleOpts);
  return parsed.swagger !== undefined ? convertSwagger2OpenAPI(parsed) : parsed;
}

function convertSwagger2OpenAPI(spec) {
  return new Promise((resolve, reject) =>
    convertObj(
      spec,
      { patch: true, warnOnly: true, text: "{}", anchors: true },
      (err, res) => {
        if (err) {
          return reject(err);
        }
        resolve(res && res.openapi);
      }
    )
  );
}

export function extractOpenApiOperations(spec) {
  const operations = Object.entries(spec.paths)
    .flatMap(([pathName, path]) =>
      Object.entries(path).flatMap(([method, path]) => {
        const name =
          path.summary ||
          path.description ||
          `${method.toUpperCase()} ${pathName}`;
        const tags = path.tags || [""];
        return tags.map((tag) => {
          return { tag, name, id: path.operationId || `${method}-${pathName}` };
        });
      })
    )
    .groupBy(({ tag }) => tag);
  const items = Object.entries(operations).flatMap(([tag, operations]) => {
    const result = [];
    if (tag !== "")
      result.push({ id: tag, name: capitalize(tag), isTag: true });
    operations.forEach(({ name, id }) => result.push({ id, name }));
    return result;
  });
  if (spec.components && spec.components.securitySchemes)
    items.unshift({
      id: "authentication",
      name: "Authentication",
      isTag: true,
    });
  return items;
}
