import { parse, registerSchemaParser } from "@asyncapi/parser";
import avroSchemaParser from "@asyncapi/avro-schema-parser";
import openapiSchemaParser from "@asyncapi/openapi-schema-parser";

registerSchemaParser(openapiSchemaParser);
registerSchemaParser(avroSchemaParser);

export async function parseAsyncAPI(input) {
  return parse(input);
}

export function extractAsyncAPIOperations(spec) {
  return Object.entries(spec.channels).flatMap(([channelName, channel]) => {
    const getOperation = (channel, type) => {
      const name = channel.summary || `${type} - ${channelName}`;
      return {
        name,
        id: channel.operationId || `${type}-${channelName}`,
      };
    };
    const result = [];
    if (channel.subscribe)
      result.push(getOperation(channel.subscribe, "subscribe"));
    if (channel.publish) result.push(getOperation(channel.publish, "publish"));
    return result;
  });
}
