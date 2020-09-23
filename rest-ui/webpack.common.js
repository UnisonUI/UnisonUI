const path = require('path')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const HtmlWebPackPlugin = require('html-webpack-plugin')
const { CleanWebpackPlugin } = require('clean-webpack-plugin')
const CopyPlugin = require('copy-webpack-plugin')
const CompressionPlugin = require('compression-webpack-plugin')

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
      automaticNameDelimiter: '-',
      chunks: 'all'
    }
  },
  module: {
    rules: [
      {
        test: /\.((png)|(svg)|(gif))(\?v=\d+\.\d+\.\d+)?$/,
        loader: 'file-loader',
        options: {
          name: 'images/[name].[sha512:hash:hex:10].[ext]',
          publicPath: '/statics/'
        }
      },
      {
        test: /\.((eot)|(woff)|(woff2)|(ttf))(\?v=\d+\.\d+\.\d+)?$/,
        loader: 'file-loader',
        options: {
          name: 'fonts/[name].[sha512:hash:hex:10].[ext]',
          publicPath: '/statics/'
        }
      },
      {
        loader: 'babel-loader',
        test: /\.jsx?$/,
        options: {
          retainLines: true,
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
    }),
    new CompressionPlugin({
      filename: '[path][base].gz[query]',
      algorithm: 'gzip',
      threshold: 10240,
      minRatio: 0.8,
      deleteOriginalAssets: false
    }),
    new CompressionPlugin({
      filename: '[path][base].br[query]',
      algorithm: 'brotliCompress',
      compressionOptions: {
        // zlib’s `level` option matches Brotli’s `BROTLI_PARAM_QUALITY` option.
        level: 11
      },
      threshold: 10240,
      minRatio: 0.8,
      deleteOriginalAssets: false
    })
  ]
}
