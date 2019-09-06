module.exports = {
  presets: [['@babel/env', { modules: 'cjs' }], '@babel/react', '@babel/flow'],
  plugins: [
    '@babel/plugin-syntax-dynamic-import',
    '@babel/plugin-proposal-class-properties',
    'add-module-exports',
  ],
  env: {
    test: {
      presets: ['@babel/env'],
      plugins: ['babel-plugin-dynamic-import-node', '@babel/plugin-transform-runtime'],
    },
  },
};
