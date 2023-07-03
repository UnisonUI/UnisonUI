import { configureStore } from "@reduxjs/toolkit";
import servicesReducer from "./features/servicesSlice";

export default configureStore({
  reducer: {
    services: servicesReducer,
  },
});
