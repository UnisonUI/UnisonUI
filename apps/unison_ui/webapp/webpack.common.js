const path = require("path");
const webpack = require("webpack");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const HtmlWebPackPlugin = require("html-webpack-plugin");
const CompressionPlugin = require("compression-webpack-plugin");
const NodePolyfillPlugin = require("node-polyfill-webpack-plugin");

module.exports = {
  entry: {
    main: path.join(__dirname, "src", "index.js"),
  },
  output: {
    path: path.join(__dirname, "..", "priv", "statics"),
    publicPath: "/statics/",
  },
  optimization: {
    splitChunks: {
      automaticNameDelimiter: "-",
      chunks: "all",
      cacheGroups: {
        react: {
          test: /[\\/]node_modules[\\/](react|react-dom|react-router-dom|@reduxjs\/toolkit)[\\/]/,
          name: "react",
          chunks: "all",
        },
        asyncapi: {
          test: /[\\/]node_modules[\\/]@asyncapi.+[\\/]/,
          name: "asyncapi",
          chunks: "all",
        },
        openapi: {
          test: /[\\/]node_modules[\\/]@redocly.+[\\/]/,
          name: "openapi",
          chunks: "all",
        },
      },
    },
  },
  resolve: {
    roots: [path.resolve(__dirname)],
    extensions: [".ts", ".tsx", ".js", ".mjs", ".json"],
    fallback: {
      fs: path.resolve(__dirname, "src/empty.js"),
    },
  },
  module: {
    rules: [
      {
        test: /\.html$/,
        loader: "html-loader",
      },
      {
        loader: "babel-loader",
        test: /\.jsx?$/,
        options: {
          retainLines: true,
          cacheDirectory: true,
        },
      },
      {
        test: /\.s?css$/,
        use: [
          MiniCssExtractPlugin.loader,
          "css-loader",
          "postcss-loader",
          "sass-loader",
        ],
      },
    ],
  },
  plugins: [
    new NodePolyfillPlugin(),
    new webpack.ProvidePlugin({
      Buffer: ["buffer", "Buffer"],
    }),
    new HtmlWebPackPlugin({
      template: path.join(__dirname, "src", "index.html"),
      filename: "index.html",
    }),
    new CompressionPlugin({
      filename: "[path][base].gz[query]",
      algorithm: "gzip",
      threshold: 10240,
      minRatio: 0.8,
      deleteOriginalAssets: false,
    }),
    new CompressionPlugin({
      filename: "[path][base].br[query]",
      algorithm: "brotliCompress",
      compressionOptions: {
        level: 11,
      },
      threshold: 10240,
      minRatio: 0.8,
      deleteOriginalAssets: false,
    }),
  ],
};
