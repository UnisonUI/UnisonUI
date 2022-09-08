const { merge } = require("webpack-merge");
const TerserPlugin = require("terser-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const CssMinimizerPlugin = require("css-minimizer-webpack-plugin");
const { CleanWebpackPlugin } = require("clean-webpack-plugin");

const common = require("./webpack.common.js");
common.plugins.unshift(new CleanWebpackPlugin());

module.exports = merge(common, {
  mode: "production",
  cache: true,
  output: {
    hashDigest: "hex",
    hashFunction: "sha512",
    filename: "js/[name].[chunkhash:10].js",
    chunkFilename: "js/[name].[chunkhash:10].js",
  },
  optimization: {
    minimizer: [
      new TerserPlugin({
        terserOptions: {
          compress: {
            drop_console: true,
          },
        },
      }),
      new MiniCssExtractPlugin({
        filename: "css/[name].[chunkhash:10].css",
        chunkFilename: "css/[chunkhash:10].css",
      }),
      new CssMinimizerPlugin(),
    ],
  },
});
