module.exports = {
  presets: [['@babel/env', {
    modules: 'cjs',
    useBuiltIns: 'entry',
    corejs: '3.0.0',
  }], '@babel/react', '@babel/flow'],
  plugins: [
    '@babel/plugin-syntax-dynamic-import',
    '@babel/plugin-proposal-class-properties',
    'add-module-exports',
  ],
  env: {
    test: {
      plugins: ['babel-plugin-dynamic-import-node'],
    },
  },
};
