export * from "./asyncapi";
export * from "./openapi";
export * from "./grpc";
export * from "./highlight";

const delimiter = "||";

export const resolveRef = (spec) => {
  const correctedSpec = structuredClone(spec);
  if (spec.components) {
    const correctedComponents = resolveComponents(spec.components);
    correctedSpec.components = correctedComponents;
  }
  Object.keys(correctedSpec)
    .filter((key) => key !== "components")
    .forEach((key) => {
      const node = correctedSpec[key];
      const refs = {};
      findRefs(node, refs, "");
      for (const path in refs) {
        let newValue;
        if (correctedSpec.components) {
          const keys = refs[path].substring(2).split("/");
          newValue = keys.reduce((acc, key) => {
            if (acc === undefined) return acc;
            return acc[key];
          }, correctedSpec);
        }
        updateObject(node, path, newValue);
      }
    });
  return correctedSpec;
};

const resolveComponents = (components) => {
  const correctedComponents = {};
  Object.entries(components).forEach(([name, components]) => {
    correctedComponents[name] = {};
    const visited = {};
    const visiting = {};
    const isCycle = (key) => {
      if (visited[key]) return false;
      if (visiting[key]) return true;
      const node = components[key];
      correctedComponents[name][key] = structuredClone(node);
      const refs = {};
      findRefs(node, refs, "");

      for (const path in refs) {
        const ref = refs[path].replace(`#/components/${name}/`, "");
        const cycled = isCycle(ref);
        updateObject(
          correctedComponents[name][key],
          path,
          cycled ? undefined : correctedComponents[name][ref]
        );
        if (cycled) return true;
      }

      visiting[key] = true;
      visiting[key] = false;
      visited[key] = true;
      return false;
    };

    for (const key in components) {
      if (!visited[key]) {
        isCycle(key);
      }
    }
  });
  return correctedComponents;
};

const updateObject = (obj, path, newValue) => {
  const correctPath =
    path && path.length && path.startsWith(delimiter)
      ? path.substring(delimiter.length)
      : path;
  const keys = correctPath.split(delimiter);
  keys.reduce((acc, key, index) => {
    if (acc === undefined) return acc;
    if (index === keys.length - 1) {
      if (newValue === undefined) delete acc[key];
      else acc[key] = newValue;
      return true;
    }
    return acc[key];
  }, obj);
};

const findRefs = (node, edges, path) => {
  if (typeof node === "object" && node) {
    if (node.$ref && edges[path] !== node.$ref) {
      edges[path] = node.$ref;
      return;
    }

    Object.entries(node).forEach(([name, value]) =>
      findRefs(value, edges, `${path}${delimiter}${name}`)
    );
  }
};
