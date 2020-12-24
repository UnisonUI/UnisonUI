const defaultTheme = require('tailwindcss/defaultTheme')
module.exports = {
  important: true, // See https://tailwindcss.com/docs/configuration#important
  purge: {
    enabled: process.env.HUGO_ENVIRONMENT === 'production',
    content: ['./hugo_stats.json'],
    mode: 'all',
    options: {
      defaultExtractor: (content) => {
        let els = JSON.parse(content).htmlElements;
        els = els.tags.concat(els.classes, els.ids);
        return els;
      }
    }
  },
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
