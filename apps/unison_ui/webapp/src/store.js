import { configureStore } from "@reduxjs/toolkit";
import requestReducer from "./features/requestSlice";
import servicesReducer from "./features/servicesSlice";

export default configureStore({
  reducer: {
    request: requestReducer,
    services: servicesReducer,
  },
});
