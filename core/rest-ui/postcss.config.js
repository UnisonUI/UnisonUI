let plugins = {
  "postcss-import": {},
  tailwindcss: {},
  "postcss-nesting": {},
  "postcss-preset-env": {
    browsers: "last 2 versions"
  },
  autoprefixer: {}
};
if (process.env.NODE_ENV === "production") {
  plugins["@fullhuman/postcss-purgecss"] = {
    content: ["src/main/js/**/*.html", "src/main/js/**/*.js"],
    defaultExtractor: content => content.match(/[A-Za-z0-9-_:/]+/g) || []
  };
}
module.exports = {
  plugins: plugins
};
