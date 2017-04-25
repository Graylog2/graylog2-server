// webpack.vendor.js
const webpack = require('webpack');
const path = require('path');
const Clean = require('clean-webpack-plugin');
const AssetsPlugin = require('assets-webpack-plugin')
const merge = require('webpack-merge');

const ROOT_PATH = path.resolve(__dirname);
const BUILD_PATH = path.resolve(ROOT_PATH, 'build');
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');

const vendorModules = require('./vendor.modules');

const TARGET = process.env.npm_lifecycle_event;
process.env.BABEL_ENV = TARGET;

console.error('Building vendor bundle.');

const webpackConfig = {
  name: 'vendor',
  entry: {
    vendor: vendorModules,
  },
  output: {
    path: BUILD_PATH,
    filename: '[name].js',
    library: '__[name]',
  },
  plugins: [
    new webpack.DllPlugin({
      path: path.resolve(MANIFESTS_PATH, '[name]-manifest.json'),
      name: '__[name]',
    }),
    new AssetsPlugin({ filename: 'vendor-module.json', path: BUILD_PATH, processOutput: function (assets) {
      const jsfiles = [];
      const cssfiles = [];
      const chunks = {};
      Object.keys(assets).forEach((chunk) => {
        if (assets[chunk].js) {
          jsfiles.push(assets[chunk].js);
        }
        if (assets[chunk].css) {
          jsfiles.push(assets[chunk].css);
        }
        chunks[chunk] = {
          size: 0,
          entry: assets[chunk].js,
          css: assets[chunk].css || []
        };
      });
      return JSON.stringify({
        files: {
          js: jsfiles,
          css: cssfiles,
          chunks: chunks
        }
      });
    } })
  ],
  recordsPath: path.resolve(ROOT_PATH, 'webpack/vendor-module-ids.json'),
};

if (TARGET === 'build') {
  module.exports = merge(webpackConfig, {
    plugins: [
      new Clean([path.resolve(BUILD_PATH)]),
      new webpack.optimize.UglifyJsPlugin({
        minimize: true,
        sourceMap: true,
        compress: {
          warnings: false,
        },
        mangle: {
          except: ['$super', '$', 'exports', 'require'],
        },
      }),
      new webpack.LoaderOptionsPlugin({
        minimize: true
      }),
    ],
    output: {
      filename: '[name].[chunkhash].js',
    },
  });
} else {
  module.exports = webpackConfig;
}
