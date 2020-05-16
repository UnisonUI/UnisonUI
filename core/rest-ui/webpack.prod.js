const merge = require("webpack-merge");
const UglifyJsPlugin = require("uglifyjs-webpack-plugin");
const OptimizeCSSAssetsPlugin = require("optimize-css-assets-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const common = require("./webpack.common.js");

module.exports = merge(common, {
  mode: "production",

  output: {
    filename: "js/[chunkhash:10].js",
    chunkFilename: "js/[chunkhash:10].js"
  },

  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        cache: true,
        parallel: true,
        sourceMap: false
      }),

      new MiniCssExtractPlugin({
        filename: "css/[chunkhash:10].css",
        chunkFilename: "css/[chunkhash:10].css"
      }),

      new OptimizeCSSAssetsPlugin({})
    ]
  }
});
