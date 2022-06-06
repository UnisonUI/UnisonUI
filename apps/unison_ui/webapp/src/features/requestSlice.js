import { createSlice } from "@reduxjs/toolkit";

const resolveVariables = (url, variables) =>
  url.replaceAll(
    /\{(\w+)\}/g,
    (match, name) => (variables && variables[name]) || match
  );
export const requestSlice = createSlice({
  name: "requests",
  initialState: {},
  reducers: {
    server: (state, { payload }) => {
      if (!state[payload.id]) state[payload.id] = { server: { variables: {} } };
      if (payload.server.url) state[payload.id].server.url = payload.server.url;
      if (payload.server.variables)
        state[payload.id].server.variables = {
          ...state[payload.id].server.variables,
          ...payload.server.variables,
        };
      if (state[payload.id].server.url)
        state[payload.id].server.computedUrl = resolveVariables(
          state[payload.id].server.url,
          state[payload.id].server.variables
        );
    },
  },
});

export const { server } = requestSlice.actions;

export const setServerUrl = (id, serverUrl) => (dispatch) =>
  dispatch(server({ id, server: { url: serverUrl } }));

export const setVariables = (id, variables) => (dispatch) =>
  dispatch(server({ id, server: { variables } }));

export default requestSlice.reducer;
