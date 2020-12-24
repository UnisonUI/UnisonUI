const defaultTheme = require('tailwindcss/defaultTheme')
module.exports = {
  purge: [
    './layouts/**/*.html'
  ],
  theme: {
    fontFamily: {
      sans: [
        'Montserrat',
        ...defaultTheme.fontFamily.sans,
      ],
      code: ['Fira Code', 'sans-serif']
    },
  }
}
