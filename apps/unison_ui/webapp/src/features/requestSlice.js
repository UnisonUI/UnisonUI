import { createSlice } from "@reduxjs/toolkit";

export const requestSlice = createSlice({
  name: "requests",
  initialState: {},
  reducers: {
    server: (state, { payload }) => {
      if (!state[payload.id]) state[payload.id] = {};
      state[payload.id].server = payload.server;
    },
  },
});
const { server } = requestSlice.actions;

export const setServer = (id, serverUrl) => (dispatch) =>
  dispatch(server({ id, server: serverUrl }));

export default requestSlice.reducer;
