export function normalizeGrpcSchema(service) {
  const spec = {
    info: {
      title: service.name,
      version: "gRPC",
      description: "",
    },
    servers: service.servers.map(({ name, address, port }) => {
      return {
        url: `${address}:${port}`,
        description: name,
      };
    }),
    services: {},
  };
  service.schema.services.forEach((service) => {
    const services = { summary: service.name, description: service.full_name };
    service.methods.forEach((method) => {
      services[method.name] = {
        summary: method.name,
        description: method.name,
      };
    });
    spec.services[service.full_name] = services;
  });
  return spec;
}

export function extractGrpcOperations(spec) {
  return Object.entries(spec.services).flatMap(([serviceName, service]) => {
    const items = [{ id: serviceName, name: service.summary }];
    Object.entries(service).forEach(([methodName, method]) => {
      if (methodName === "summary" || methodName === "description") return;
      items.push({ id: `${service.summary}-${methodName}`, name: methodName });
    });
    return items;
  });
}

export const isMap = (schema) =>
  schema.options && schema.options.map_entry === "true";

export const isDeprecated = (field) =>
  field.options && field.options.deprecated === "true";

const getValue = (field, schema) => {
  switch (field.type) {
    case "STRING":
      return "STRING";
    case "BYTES":
      return btoa("BYTES");
    case "BOOL":
      return true;
    case "MESSAGE":
      return messageExample(schema, field.schema);
    case "ENUM":
      return schema.enums.find((e) => e.name === field.schema).values[0];
    case "FLOAT":
    case "DOUBLE":
      return 0.5;
    default:
      return 42;
  }
};

export const messageExample = (schema, method) => {
  const result = {};
  const message = schema.messages.find((message) => message.name === method);
  const fields = message.fields;
  const oneOf = message.oneOf;
  for (const field of fields) {
    let value =
      field.default !== undefined ? this.default : getValue(field, schema);
    if (field.label === "repeated") {
      const subSchema = schema.messages.find(
        (message) => message.name === field.schema
      );
      if (isMap(subSchema)) {
        value = {};
        const key = getValue(subSchema.fields.find((f) => f.name === "key"));
        const val = getValue(subSchema.fields.find((f) => f.name === "value"));
        value[key] = val;
      } else value = [value];
    }
    result[field.name] = value;
  }
  for (const [name, fields] of Object.entries(oneOf)) {
    const field = fields[0];
    result[name] = { type: field.name, value: getValue(field, schema) };
  }
  return result;
};

export const stringify = (object) => {
  if (typeof object === "string") return object;
  return JSON.stringify(object, null, 2);
};
