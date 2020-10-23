module.exports = {
  future: {
    removeDeprecatedGapUtilities: true,
    purgeLayersByDefault: true
  },
  plugins: [require('tailwindcss-neumorphism')],
  purge: false,
  theme: {
    screens: {
      sm: '640px',
      md: '768px',
      lg: '1024px',
      xl: '1280px'
    },
    fontFamily: {
      body: ['Montserrat', 'sans-serif'],
      code: ['Fira Code', 'sans-serif']
    },
    minWidth: {
      0: '0',
      '1/4': '25%',
      '1/2': '50%',
      '3/4': '75%',
      full: '100%'
    },
    borderWidth: {
      default: '1px',
      0: '0',
      2: '2px',
      4: '4px'
    },
    extend: {
      colors: {
        cyan: '#9cdbff'
      },
      spacing: {
        96: '24rem',
        128: '32rem'
      }
    }
  }
}
