const path = require('path')

const plugins = [
  require('postcss-font-magician'),
  require('tailwindcss'),
  require('autoprefixer')
]

if (process.env.NODE_ENV === 'production') {
  plugins.push(require('@fullhuman/postcss-purgecss')({
    content: [
      'src/**/*.html',
      'src/**/*.js',
      'node_modules/react-burger-menu/**/*.js',
      'node_modules/swagger-ui/**/*.js',
      'node_modules/swagger-ui-react/**/*.js',
      'node_modules/@asyncapi/**/*.js'
    ],
    safelist: [/swagger-ui/, /opblock/, /opblock-summary/],
    defaultExtractor: content => content.match(/[A-Za-z0-9-_:/]+/g) || []
  }))
}

module.exports = {
  syntax: 'postcss-scss',
  plugins: plugins
}
