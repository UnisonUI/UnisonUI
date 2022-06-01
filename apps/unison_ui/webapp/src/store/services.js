import { configureStore } from "@reduxjs/toolkit";
import servicesReducer from "../features/services/servicesSlice";

export default configureStore({
  reducer: {
    services: servicesReducer,
  },
});
