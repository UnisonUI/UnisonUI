import { createSlice } from "@reduxjs/toolkit";
import { normalizeGrpcSchema, parseAsyncAPI, parseOpenApi } from "../utils";

export const servicesSlice = createSlice({
  name: "services",
  initialState: {},
  reducers: {
    add: (state, { payload }) => {
      if (!state[payload.name]) {
        state[payload.name] = [];
      }

      if (!state[payload.name].find((item) => item.id === payload.id)) {
        state[payload.name].push({
          id: payload.id,
          name: payload.name,
          metadata: payload.metadata,
          useProxy: payload.useProxy,
          type: payload.type,
          spec: payload.spec,
        });
      } else {
        state[payload.name] = state[payload.name].map((service) => {
          if (service.id !== payload.id) return service;
          else {
            return {
              id: payload.id,
              name: payload.name,
              metadata: payload.metadata,
              useProxy: payload.useProxy,
              type: payload.type,
              spec: payload.spec,
            };
          }
        });
      }
    },
    remove: (state, { payload }) => {
      state = Object.entries(state).reduce((obj, [name, newServices]) => {
        const filteredServices = newServices.filter(
          (item) => item.id !== payload.id
        );
        if (filteredServices.length) {
          obj[name] = filteredServices;
        }
        return obj;
      }, {});
    },
  },
});
export const { add, remove } = servicesSlice.actions;
export const handleEvent = (data) => (dispatch) => {
  switch (data.event) {
    case "serviceUp":
      switch (data.type) {
        case "openapi":
          parseOpenApi(data.content)
            .then((spec) => {
              data.spec = spec;
              dispatch(add(data));
            })
            .catch(console.error);
          break;
        case "asyncapi":
          parseAsyncAPI(data.content)
            .then((spec) => {
              data.spec = spec._json;
              dispatch(add(data));
            })
            .catch(console.error);
          break;
        case "grpc":
          data.spec = normalizeGrpcSchema(data);
          console.log(data);
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
