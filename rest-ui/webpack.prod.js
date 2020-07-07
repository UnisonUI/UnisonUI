const { merge } = require("webpack-merge");
const TerserPlugin = require("terser-webpack-plugin");
const OptimizeCSSAssetsPlugin = require("optimize-css-assets-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const common = require("./webpack.common.js");

module.exports = merge(common, {
  mode: "production",
  cache: true,
  output: {
    hashDigest: "hex",
    hashFunction: "sha512",
    filename: "js/[chunkhash:10].js",
    chunkFilename: "js/[chunkhash:10].js"
  },

  optimization: {
    minimizer: [
      new TerserPlugin({
        parallel: 4,
        sourceMap: false,
        terserOptions: {
          compress: {
            drop_console: true
          }
        }
      }),

      new MiniCssExtractPlugin({
        hashDigest: "hex",
        hashFunction: "sha512",
        filename: "css/[chunkhash:10].css",
        chunkFilename: "css/[chunkhash:10].css"
      }),

      new OptimizeCSSAssetsPlugin({})
    ]
  }
});
