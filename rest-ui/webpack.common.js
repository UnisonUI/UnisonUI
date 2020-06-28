const path = require('path')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const HtmlWebPackPlugin = require('html-webpack-plugin')
const { CleanWebpackPlugin } = require('clean-webpack-plugin')
const CopyPlugin = require('copy-webpack-plugin')

module.exports = {
  entry: {
    main: path.join(__dirname, 'src', 'main', 'js', 'index.js')
  },
  output: {
    path: path.join(__dirname, 'src', 'main', 'resources', 'web', 'statics'),
    publicPath: '/statics'
  },
  optimization: {
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        vendor: {
          test: /[\\/]node_modules[\\/](react|react-dom)[\\/]/,
          name: 'react',
          chunks: 'all'
        }
      }
    }
  },
  module: {
    rules: [
      {
        test: /\.((png)|(svg)|(gif))(\?v=\d+\.\d+\.\d+)?$/,
        loader: 'file-loader',
        options: { name: 'images/[name]-[sha512:hash:base64:7].[ext]', publicPath: '/statics/' }
      },
      {
        test: /\.((eot)|(woff)|(woff2)|(ttf))(\?v=\d+\.\d+\.\d+)?$/,
        loader: 'file-loader',
        options: { name: 'fonts/[name]-[sha512:hash:base64:7].[ext]', publicPath: '/statics/' }
      },
      { test: /\.json$/, loader: 'json-loader' },
      {
        loader: 'babel-loader',
        test: /\.jsx?$/,
        options: {
          cacheDirectory: true
        }
      },
      {
        test: /\.css$/,
        use: [
          'style-loader',
          MiniCssExtractPlugin.loader,
          'css-loader',
          'postcss-loader'
        ]
      }
    ]
  },

  plugins: [
    new CleanWebpackPlugin(),
    new CopyPlugin({
      patterns: [{ from: './src/main/js/images', to: 'images' }]
    }),
    new HtmlWebPackPlugin({
      template: './src/main/js/index.html',
      filename: '../index.html'
    })
  ]
}
