// eslint-disable-next-line import/no-internal-modules
import { bundle } from "@redocly/openapi-core/lib/bundle";
// eslint-disable-next-line import/no-internal-modules
import { Config } from "@redocly/openapi-core/lib/config/config";
/* tslint:disable-next-line:no-implicit-dependencies */
import { convertObj } from "swagger2openapi";
import { parseYaml } from "./yaml";

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

export function convertSwagger2OpenAPI(spec) {
  return new Promise((resolve, reject) =>
    convertObj(
      spec,
      { patch: true, warnOnly: true, text: "{}", anchors: true },
      (err, res) => {
        // TODO: log any warnings
        if (err) {
          return reject(err);
        }
        resolve(res && res.openapi);
      }
    )
  );
}
