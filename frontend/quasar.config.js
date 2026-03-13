/* eslint-env node */
const { configure } = require('quasar/wrappers')

module.exports = configure(function (/* ctx */) {
  return {
    eslint: { warnings: true, errors: true },

    boot: ['pinia', 'axios'],

    css: ['app.scss'],

    extras: ['roboto-font', 'material-icons'],

    build: {
      target: {
        browser: ['es2019', 'edge88', 'firefox78', 'chrome87', 'safari13.1'],
        node: 'node20',
      },
      vueRouterMode: 'hash',
    },

    devServer: {
      open: false,
      proxy: {
        '/api': {
          target: process.env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },

    framework: {
      config: {},
      plugins: ['Notify', 'Loading', 'LocalStorage', 'Dialog'],
    },

    animations: [],

    ssr: { pwa: false },
    pwa: {},
    cordova: {},
    capacitor: { hideSplashscreen: true },
    electron: { bundler: 'packager' },
    bex: {},
  }
})
