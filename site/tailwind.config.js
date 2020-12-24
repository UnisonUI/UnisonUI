const defaultTheme = require('tailwindcss/defaultTheme')
module.exports = {
  purge: false,
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
