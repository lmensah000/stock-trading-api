/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx}", "./public/index.html"],
  theme: {
    extend: {
      colors: {
        'robin-green': '#00C805',
        'robin-red': '#FF5000',
        'robin-dark': '#1E2124',
        'robin-darker': '#141518',
        'robin-card': '#1E2124',
        'robin-border': '#2F3336',
        'robin-text': '#C9CACC',
        'robin-text-light': '#FFFFFF'
      }
    }
  },
  plugins: []
};
