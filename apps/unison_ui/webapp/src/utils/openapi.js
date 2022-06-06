import loadable from "@loadable/component";
/* tslint:disable-next-line:no-implicit-dependencies */
import { convertObj } from "swagger2openapi";
import { parseYaml } from "./yaml";
import capitalize from "lodash-es/capitalize";

const { bundle } = loadable.lib(() =>
  import("@redocly/openapi-core/lib/bundle")
);

const { Config } = loadable.lib(() =>
  import("@redocly/openapi-core/lib/config/config")
);

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
        const name = path.summary || `${pathName} - ${method.toUpperCase()}`;
        const tags = path.tags || [""];
        return tags.map((tag) => {
          return { tag, name, id: path.operationId || `${method}-${pathName}` };
        });
      })
    )
    .groupBy(({ tag }) => tag);
  return Object.entries(operations).flatMap(([tag, operations]) => {
    const result = [];
    if (tag !== "") result.push({ id: tag, name: capitalize(tag) });
    operations.forEach(({ name, id }) => result.push({ id, name }));
    return result;
  });
}
