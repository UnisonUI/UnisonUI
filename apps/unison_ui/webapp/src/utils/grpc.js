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
