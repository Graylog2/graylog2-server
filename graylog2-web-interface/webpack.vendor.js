// webpack.config.js
const webpack = require('webpack');
const path = require('path');
const Clean = require('clean-webpack-plugin');

const ROOT_PATH = path.resolve(__dirname);
const BUILD_PATH = path.resolve(ROOT_PATH, 'build');

const vendorModules = require('./vendor.modules');

console.log('Building vendor bundle.');

const webpackConfig = {
  entry: {
    //app: APP_PATH,
    //config: 'config.js',
    vendor: vendorModules,
  },
  output: {
    path: BUILD_PATH,
    filename: '[name].js',
    publicPath: '/',
    library: '__[name]',
  },
  plugins: [
    new Clean([path.resolve(BUILD_PATH, 'vendor.*.js')]),
    new webpack.DllPlugin({
      path: path.resolve(BUILD_PATH, '[name]-manifest.json'),
      name: '__[name]',
    }),
  ],
  recordsPath: path.resolve(ROOT_PATH, 'webpack/vendor-module-ids.json'),
};

module.exports = webpackConfig;
