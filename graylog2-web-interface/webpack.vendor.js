// webpack.vendor.js
const webpack = require('webpack');
const path = require('path');
const Clean = require('clean-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const ROOT_PATH = path.resolve(__dirname);
const BUILD_PATH = path.resolve(ROOT_PATH, 'build');
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');

const vendorModules = require('./vendor.modules');

console.error('Building vendor bundle.');

const webpackConfig = {
  name: 'vendor',
  entry: {
    vendor: vendorModules,
  },
  output: {
    path: BUILD_PATH,
    filename: '[name].[chunkhash].js',
    library: '__[name]',
  },
  plugins: [
    new Clean([path.resolve(BUILD_PATH, 'vendor.*.js')]),
    new webpack.DllPlugin({
      path: path.resolve(MANIFESTS_PATH, '[name]-manifest.json'),
      name: '__[name]',
    }),
    new HtmlWebpackPlugin({ filename: 'vendor-module.json', inject: false, template: path.resolve(ROOT_PATH, 'templates/module.json.template') }),
  ],
  recordsPath: path.resolve(ROOT_PATH, 'webpack/vendor-module-ids.json'),
};

module.exports = webpackConfig;
