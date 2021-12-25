const path = require('path')
const webpack = require('webpack')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const HtmlWebPackPlugin = require('html-webpack-plugin')
const CopyPlugin = require('copy-webpack-plugin')
const CompressionPlugin = require('compression-webpack-plugin')

module.exports = {
  entry: {
    main: path.join(__dirname, 'src', 'index.js')
  },
  output: {
    path: path.join(__dirname, '..', 'priv', 'statics'),
    publicPath: '/statics/'
  },
  optimization: {
    splitChunks: {
      automaticNameDelimiter: '-',
      chunks: 'all'
    }
  },
  resolve: {
    fallback: {
      buffer: require.resolve('buffer/'),
      stream: require.resolve("stream-browserify")
    }
  },
  module: {
    rules: [{
      test: /\.html$/,
      loader: 'html-loader'
    }, {
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
        MiniCssExtractPlugin.loader,
        'css-loader',
        'postcss-loader'
      ]
    }
    ]
  },

  plugins: [
    new webpack.ProvidePlugin({
      Buffer: ['buffer', 'Buffer']
    }),
    new CopyPlugin({
      patterns: [{
        from: './src/images',
        to: 'images'
      }]
    }),
    new HtmlWebPackPlugin({
      template: path.join(__dirname, 'src', 'index.html'),
      filename: 'index.html'
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
        level: 11
      },
      threshold: 10240,
      minRatio: 0.8,
      deleteOriginalAssets: false
    })
  ]
}
