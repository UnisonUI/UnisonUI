import { createSlice } from "@reduxjs/toolkit";
import { normalizeGrpcSchema, parseAsyncAPI, parseOpenApi } from "../utils";
import { toast } from "react-toastify";
import { server } from "./requestSlice";

export const servicesSlice = createSlice({
  name: "services",
  initialState: {},
  reducers: {
    add: (state, { payload }) => {
      if (!state[payload.name]) {
        state[payload.name] = [];
      }

      const newService = {
        id: payload.id,
        name: payload.name,
        metadata: payload.metadata,
        useProxy: payload.useProxy,
        type: payload.type,
        spec: payload.spec,
      };

      if (!state[payload.name].find((item) => item.id === payload.id))
        state[payload.name].push(newService);
      else
        state[payload.name] = state[payload.name].map((service) => {
          if (service.id !== payload.id) return service;
          return newService;
        });
    },
    remove: (state, { payload }) => {
      Object.entries(state).forEach(([name, newServices]) => {
        const filteredServices = newServices.filter(
          (item) => item.id !== payload.id
        );
        if (filteredServices.length) {
          state[name] = filteredServices;
        } else {
          delete state[name];
        }
      });
    },
  },
});

const { add, remove } = servicesSlice.actions;

export const handleEvent = (data) => (dispatch) => {
  switch (data.event) {
    case "serviceUp":
    case "serviceChanged":
      switch (data.type) {
        case "openapi":
          parseOpenApi(data.content)
            .then((spec) => {
              data.spec = spec;
              dispatch(add(data));

              if (spec.servers) {
                const variables = {};
                spec.servers[0].variables &&
                  Object.entries(spec.servers[0].variables).forEach(
                    ([name, variable]) => {
                      variables[name] = variable.default;
                    }
                  );
                dispatch(
                  server({
                    id: data.id,
                    server: { url: spec.servers[0].url, variables },
                  })
                );
              }
            })
            .catch((error) => {
              console.error(error);
              toast.error(`${data.name}: ${error}`, { autoClose: 5000 });
            });
          break;
        case "asyncapi":
          parseAsyncAPI(data.content)
            .then((spec) => {
              data.spec = spec._json;
              dispatch(add(data));
            })
            .catch((error) =>
              toast.error(`${data.name}: ${error}`, { autoClose: 5000 })
            );
          break;
        case "grpc":
          data.spec = normalizeGrpcSchema(data);
          // console.log(data);
          dispatch(add(data));
      }
      break;

    case "serviceDown":
      dispatch(remove(data));
      break;
  }
};

export const selectAllServices = (state) => state.services;

export const fetchService = (state, id) => {
  const service = Object.values(state.services)
    .flat()
    .find((service) => service.id === id);
  if (service) return service;
  return null;
};

export default servicesSlice.reducer;
