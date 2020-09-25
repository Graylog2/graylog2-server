module.exports = {
  presets: [['@babel/env', { modules: false }], '@babel/react', '@babel/flow'],
  plugins: [
    '@babel/plugin-syntax-dynamic-import',
    '@babel/plugin-proposal-class-properties',
    'babel-plugin-styled-components',
  ],
  env: {
    test: {
      presets: ['@babel/env'],
      plugins: [
        'babel-plugin-dynamic-import-node',
        '@babel/plugin-transform-runtime',
        'add-module-exports',
      ],
    },
  },
};
