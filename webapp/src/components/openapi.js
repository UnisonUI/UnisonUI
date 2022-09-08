import React from "react";
import { useLocation } from "react-router-dom";
import { useSelector } from "react-redux";
import { fetchSpec } from "../features/services/servicesSlice";

export default function OpenAPI() {
  const location = useLocation();
  const id = location.pathname.replace("/service/", "");
  const spec = useSelector((state) => fetchSpec(state, id));
  console.log(spec);
  return <div />;
}
/*
const theme = () => {
  return { syntaxHighlight: { activated: true, theme: "obsidian" } };
};

const ProxyPlugin = {
  proxy: (system) => {
    return {
      wrapComponents: {
        curl: (Original, system) => (props) => {
          const url = Buffer.from(
            props.request.get("url").substring(7),
            "base64"
          );
          const request = props.request.set("url", url);
          return <Original request={request} getConfigs={props.getConfigs} />;
        },
      },
    };
  },
};

const UnisonUILayoutPlugin = {
  unisonui: (system) => {
    return {
      components: {
        InfoUrl: () => null,
      },
      wrapComponents: {
        highlightCode: (Original, system) => (props) => {
          return <Original {...props} getConfigs={theme} />;
        },
      },
    };
  },
};

function OpenAPI(props) {
  const location = useLocation();
  const useProxy = !!props.useProxy;
  const id = location.pathname.substring(1);
  const plugins = [UnisonUILayoutPlugin];
  if (useProxy) plugins.push(ProxyPlugin);
  const requestInterceptor = (r) => {
    if (useProxy && r.url.startsWith("http")) {
      r.url = `/proxy/${r.url.toString("base64")}`;
    }
    return r;
  };
  return (
    <div>
      {/* <SwaggerUI
        url={"/services/" + id}
        requestInterceptor={requestInterceptor}
        plugins={plugins}
      /> }
    </div>
  );
}

export default OpenAPI;
*/
