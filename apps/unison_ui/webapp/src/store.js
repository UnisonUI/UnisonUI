import { configureStore } from "@reduxjs/toolkit";
import requestReducer from "./features/requestSlice";
import servicesReducer from "./features/servicesSlice";

export default configureStore({
  reducer: {
    service: requestReducer,
    services: servicesReducer,
  },
});
