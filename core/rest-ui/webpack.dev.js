const merge = require("webpack-merge");
const path = require("path");
const { CleanWebpackPlugin } = require("clean-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const common = require("./webpack.common");
module.exports = merge(common, {
  mode: "development",

  output: {
    filename: "js/[name].js",
    chunkFilename: "js/[id].bundle.js"
  },

  devServer: {
    port: process.env.PORT || 3000,
    contentBase: path.join(process.cwd(), "./src/main/resources/web/"),
    watchContentBase: true,
    quiet: false,
    open: false,
    historyApiFallback: {
      rewrites: [{ from: /./, to: "404.html" }]
    }
  },

  plugins: [
    new CleanWebpackPlugin({
      cleanOnceBeforeBuildPatterns: [
        "public/**/*.js",
        "public/**/*.css",
        "content/webpack.json",
        "tailwind.config.js"
      ]
    }),

    new MiniCssExtractPlugin({
      filename: "css/[name].css",
      chunkFilename: "css/[id].css"
    })
  ]
});
