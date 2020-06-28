const plugins = {
  'postcss-import': { path: ['src/main/js/css'] },
  'postcss-mixins': { mixinsDir: 'src/main/js/css/mixins' },
  'postcss-simple-vars': {},
  'postcss-url': {},
  tailwindcss: {},
  'postcss-font-magician': {},
  'postcss-nested': {},
  'postcss-preset-env': {
    browsers: 'last 2 versions'
  },
  autoprefixer: {}
}

if (process.env.NODE_ENV === 'production') {
  plugins['@fullhuman/postcss-purgecss'] = {
    content: [
      'src/main/js/**/*.html',
      'src/main/js/**/*.js',
      'node_modules/react-burger-menu/**/*.js',
      'node_modules/swagger-ui/**/*.js'
    ],
    whitelistPatterns: [/swagger-ui/, /opblock/, /opblock-summary/],
    defaultExtractor: content => content.match(/[A-Za-z0-9-_:/]+/g) || []
  }
}

module.exports = {
  plugins: plugins
}
