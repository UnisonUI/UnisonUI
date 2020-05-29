const merge = require('webpack-merge')
const { CleanWebpackPlugin } = require('clean-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')

const common = require('./webpack.common')

module.exports = merge(common, {
  mode: 'development',

  output: {
    filename: 'js/[name].js',
    chunkFilename: 'js/[id].bundle.js'
  },
  devtool: 'source-map',
  plugins: [
    new MiniCssExtractPlugin({
      filename: 'css/[name].css',
      chunkFilename: 'css/[id].css'
    })
  ]
})
