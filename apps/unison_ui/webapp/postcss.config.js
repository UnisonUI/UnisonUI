const plugins = [
  require("tailwindcss"),
  require("postcss-font-magician")(),
  require("autoprefixer"),
];

if (process.env.NODE_ENV === "production") {
  plugins.push(
    require("@fullhuman/postcss-purgecss")({
      content: [
        "src/**/*.html",
        "src/**/*.js",
        "node_modules/react-toastify/**/*.js",
      ],
      safelist: { deep: [/unisonui/] },
      defaultExtractor: (content) => content.match(/[A-Za-z0-9-_:/]+/g) || [],
    })
  );
}

module.exports = {
  syntax: "postcss-scss",
  plugins,
};
