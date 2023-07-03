import { Parser } from "@asyncapi/parser";
import { AvroSchemaParser } from "@asyncapi/avro-schema-parser";
import { OpenAPISchemaParser } from "@asyncapi/openapi-schema-parser";
import capitalize from "lodash-es/capitalize";

const parser = new Parser();
parser.registerSchemaParser(OpenAPISchemaParser());
parser.registerSchemaParser(AvroSchemaParser());

export async function parseAsyncAPI(input) {
  const { document } = await parser.parse(input);
  return document.json();
}

export function extractAsyncAPIOperations(spec) {
  const operations = Object.entries(spec.channels)
    .flatMap(([channelName, channel]) => {
      const getOperation = (channel, type) => {
        const name =
          channel.summary || channel.description || `${type} - ${channelName}`;
        const tags = channel.tags || [""];
        return tags.map((tag) => {
          return {
            tag,
            name,
            id: channel.operationId || `${type}-${channelName}`,
            deprecated: channel.deprecated,
          };
        });
      };
      const result = [];
      if (channel.subscribe)
        getOperation(channel.subscribe, "subscribe").forEach((item) =>
          result.push(item)
        );

      if (channel.publish)
        getOperation(channel.publish, "publish").forEach((item) =>
          result.push(item)
        );
      return result;
    })
    .groupBy(({ tag }) => tag);

  return Object.entries(operations).flatMap(([tag, operations]) => {
    const result = [];
    if (tag !== "")
      result.push({ id: tag, name: capitalize(tag), isTag: true });
    operations.forEach(({ name, id, deprecated }) =>
      result.push({ id, name, deprecated })
    );
    return result;
  });
}
