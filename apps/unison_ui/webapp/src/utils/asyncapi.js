import { parse, registerSchemaParser } from "@asyncapi/parser";
import avroSchemaParser from "@asyncapi/avro-schema-parser";
import openapiSchemaParser from "@asyncapi/openapi-schema-parser";

registerSchemaParser(openapiSchemaParser);
registerSchemaParser(avroSchemaParser);

export async function parseAsyncAPI(input) {
  return parse(input);
}
