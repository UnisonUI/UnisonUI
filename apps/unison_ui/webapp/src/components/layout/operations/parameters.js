import React from "react";
const paramsType = ["path", "header", "query", "cookie"];

export const Parameters = ({ parameters }) => {
  if (!parameters) return;
  const parametersByType = parameters
    .filter((param) => {
      const name = param.name.toLowerCase();
      return (
        param.in !== "header" ||
        name === "accept" ||
        name === "content-type" ||
        name === "authorization"
      );
    })
    .groupBy((param) => param.in);
  console.log(parametersByType);
  console.log(paramsType);
  return (
    <div>
      {paramsType.map(
        (type, i) =>
          parametersByType[type] && (
            <div className="section">
              <h1 className="title">{getType(type)}</h1>
              <div className="section-content">
                {parametersByType[type].map((parameter, j) => (
                  <Parameter
                    parameter={parameter}
                    key={`parameter-${type}-${i}-${j}`}
                  />
                ))}
              </div>
            </div>
          )
      )}
    </div>
  );
};
const getType = (type) => {
  switch (type) {
    case "path":
      return "PATH PARAMETERS";
    case "header":
      return "REQUEST HEADERS";
    case "query":
      return "QUERY-STRING PARAMETERS";
    case "cookie":
      return "REQUEST COOKIES";
  }
};

const Parameter = ({ parameter }) => {
  return (
    <div className="flex flex-row justify-between">
      <div className="flex flex-col justify-end items-end">
        <div>
          {parameter.required && <span className="text-red-500 mr-2">*</span>}{" "}
          {parameter.deprecrated ? (
            <span className="text-red-500">{parameter.name}</span>
          ) : (
            parameter.name
          )}
        </div>
        <div>
          {parameter.schema.format
            ? parameter.schema.format
            : parameter.schema.type}
        </div>
      </div>
      <div>
        <input type="text" className="w-full" />
      </div>
    </div>
  );
};
