const path = require('path')

const plugins = {
  'postcss-import': {
    path: ['src/css']
  },
  'postcss-mixins': {
    mixinsDir: path.join(__dirname, 'src', 'css', 'mixins')
  },
  'postcss-url': {},
  'postcss-font-magician': {},
  'tailwindcss/nesting': {},
  'tailwindcss': {},
  'postcss-preset-env': {
    stage: 0,
    browsers: 'last 2 versions'
  }
}

if (process.env.NODE_ENV === 'production') {
  plugins['@fullhuman/postcss-purgecss'] = {
    content: [
      'src/**/*.html',
      'src/**/*.js',
      'node_modules/react-burger-menu/**/*.js',
      'node_modules/swagger-ui/**/*.js',
      'node_modules/swagger-ui-react/**/*.js'
    ],
    safelist: [/swagger-ui/, /opblock/, /opblock-summary/],
    defaultExtractor: content => content.match(/[A-Za-z0-9-_:/]+/g) || []
  }
}

module.exports = {
  plugins: plugins
}
