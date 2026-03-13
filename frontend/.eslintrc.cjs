/* eslint-env node */
const { resolve } = require('path')

module.exports = {
  root: true,
  parserOptions: { ecmaVersion: 'latest' },
  env: { browser: true },
  extends: [
    'plugin:vue/vue3-essential',
    'eslint:recommended',
    '@vue/eslint-config-typescript/recommended',
  ],
  rules: {
    '@typescript-eslint/no-explicit-any': 'off',
    'vue/multi-word-component-names': 'off',
  },
}
