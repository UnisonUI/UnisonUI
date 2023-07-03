module.exports = {
  future: {
    removeDeprecatedGapUtilities: true,
  },
  content: ["./src/**/*.js"],
  plugins: [require("@tailwindcss/typography")],
  theme: {
    screens: {
      sm: "640px",
      md: "768px",
      lg: "1024px",
      xl: "1280px",
    },
    fontFamily: {
      body: ["'Poppins'", "sans-serif"],
      code: ["'Fira Code'", "sans-serif"],
    },
    minWidth: {
      0: "0",
      "1/4": "25%",
      "1/2": "50%",
      "3/4": "75%",
      full: "100%",
    },
    extend: {
      colors: {
        cyan: "#9cdbff",
        background: "var(--background)",
        primary: "var(--primary)",
        "background-shadow": "var(--background-shadow)",
        "background-highlight": "var(--background-highlight)",
        "primary-shadow": "var(--primary-shadow)",
        "primary-highlight": "var(--primary-highlight)",
      },
      spacing: {
        96: "24rem",
        128: "32rem",
      },
    },
  },
};