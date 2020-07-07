const { merge } = require('webpack-merge')
const TerserPlugin = require('terser-webpack-plugin')
const OptimizeCSSAssetsPlugin = require('optimize-css-assets-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')

const common = require('./webpack.common.js')

module.exports = merge(common, {
  mode: 'production',
  cache: true,
  output: {
    hashDigest: 'hex',
    hashFunction: 'sha512',
    filename: 'js/[name].[chunkhash:10].js',
    chunkFilename: 'js/[name].[chunkhash:10].js'
  },

  optimization: {
    minimizer: [
      new TerserPlugin({
        parallel: 4,
        cache: true,
        sourceMap: false,
        terserOptions: {
          compress: {
            drop_console: true
          }
        }
      }),

      new MiniCssExtractPlugin({
        hashDigest: 'hex',
        hashFunction: 'sha512',
        filename: 'css/[name].[chunkhash:10].css',
        chunkFilename: 'css/[chunkhash:10].css'
      }),

      new OptimizeCSSAssetsPlugin({})
    ]
  }
})
