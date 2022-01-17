const path = require('path')

const plugins = [
  // require('postcss-import')({
  //   path: [path.join(__dirname, 'src', 'css')]
  // }),
  // require('postcss-mixins')({
  //   mixinsDir: path.join(__dirname, 'src', 'css', 'mixins')
  // }),
  require('postcss-url'),
  require('postcss-font-magician'),
  require('tailwindcss/nesting'),
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
      'node_modules/swagger-ui-react/**/*.js'
    ],
    safelist: [/swagger-ui/, /opblock/, /opblock-summary/],
    defaultExtractor: content => content.match(/[A-Za-z0-9-_:/]+/g) || []
  }))
}

module.exports = {
  syntax: 'postcss-scss',
  plugins: plugins
}
