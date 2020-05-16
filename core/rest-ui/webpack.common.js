const webpack = require("webpack");
const path = require("path");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const HtmlWebPackPlugin = require("html-webpack-plugin");

module.exports = {
  entry: {
    main: path.join(__dirname, "src", "main", "js", "index.js")
  },

  output: {
    path: path.join(__dirname, "src", "main", "resources", "web", "statics"),
    publicPath: "/statics"
  },
  stats: {
    colors: true,
    hash: true,
    timings: true,
    assets: true,
    chunks: true,
    chunkModules: true,
    modules: true,
    children: true
  },
  optimization: {
    splitChunks: {
      cacheGroups: {
        default: false,
        vendors: false,
        react: {
          chunks: "all",
          name: "react",
          test: /react/,
          priority: 20
        },
        swagger: {
          chunks: "all",
          name: "swagger",
          test: /swagger/,
          priority: 20
        },
        vendor: {
          name: "vendor",
          chunks: "all",
          test: /node_modules/,
          priority: 10
        }
      }
    }
  },
  module: {
    rules: [
      {
        test: /\.((png)|(eot)|(woff)|(woff2)|(ttf)|(svg)|(gif))(\?v=\d+\.\d+\.\d+)?$/,
        loader: "file-loader",
        options: { name: "[hash].[ext]", publicPath: "/" }
      },
      { test: /\.json$/, loader: "json-loader" },
      {
        loader: "babel-loader",
        test: /\.jsx?$/,
        options: {
          cacheDirectory: true
        }
      },
      {
        test: /\.(sa|sc|c)ss$/,
        use: [
          "style-loader",
          MiniCssExtractPlugin.loader,
          "css-loader",
          "postcss-loader"
        ]
      }
    ]
  },

  plugins: [
    new HtmlWebPackPlugin({
      template: "./src/main/js/index.html",
      filename: "../index.html"
    })
  ]
};
