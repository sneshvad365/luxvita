/* eslint-env node */
const { configure } = require('quasar/wrappers')

module.exports = configure(function (/* ctx */) {
  return {
    eslint: { warnings: true, errors: true },

    boot: ['pinia', 'axios'],

    css: ['app.scss'],

    extras: ['roboto-font', 'material-icons'],

    build: {
      publicPath: '/luxvita/',
      target: {
        browser: ['es2019', 'edge88', 'firefox78', 'chrome87', 'safari13.1'],
        node: 'node20',
      },
      vueRouterMode: 'hash',
    },

    devServer: {
      open: false,
      allowedHosts: 'all',
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
    pwa: {
      manifest: {
        name: 'LuxVita',
        short_name: 'LuxVita',
        display: 'standalone',
        orientation: 'portrait',
        background_color: '#ffffff',
        theme_color: '#1976D2',
        icons: [
          { src: 'icons/icon-128x128.png', sizes: '128x128', type: 'image/png' },
          { src: 'icons/icon-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icons/icon-256x256.png', sizes: '256x256', type: 'image/png' },
          { src: 'icons/icon-384x384.png', sizes: '384x384', type: 'image/png' },
          { src: 'icons/icon-512x512.png', sizes: '512x512', type: 'image/png' },
        ],
      },
    },
    cordova: {},
    capacitor: { hideSplashscreen: true },
    electron: { bundler: 'packager' },
    bex: {},
  }
})
