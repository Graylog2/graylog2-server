// webpack.config.js
const webpack = require('webpack');
const path = require('path');
const Clean = require('clean-webpack-plugin');

const ROOT_PATH = path.resolve(__dirname);
const BUILD_PATH = path.resolve(ROOT_PATH, 'build');
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');
const VENDOR_MANIFEST = require(path.resolve(MANIFESTS_PATH, 'vendor-manifest.json'));

console.log('Building shared bundle.');

const webpackConfig = {
  entry: {
    shared: ['components/common', 'util', 'logic'],
  },
  output: {
    path: BUILD_PATH,
    filename: '[name].js',
    publicPath: '/',
    library: '__[name]',
  },
  module: {
    preLoaders: [
      // { test: /\.js(x)?$/, loader: 'eslint-loader', exclude: /node_modules|public\/javascripts/ }
    ],
    loaders: [
      { test: /\.json$/, loader: 'json-loader' },
      { test: /\.js(x)?$/, loaders: ['babel-loader'], exclude: /node_modules|\.node_cache/ },
      { test: /\.ts$/, loader: 'babel-loader!ts-loader', exclude: /node_modules|\.node_cache/ },
      { test: /\.(woff(2)?|svg|eot|ttf|gif|jpg)(\?.+)?$/, loader: 'file-loader' },
      { test: /\.png$/, loader: 'url-loader' },
      { test: /\.less$/, loader: 'style!css!less' },
      { test: /\.css$/, loader: 'style!css' },
    ],
  },
  resolve: {
    // you can now require('file') instead of require('file.coffee')
    extensions: ['', '.js', '.json', '.jsx', '.ts'],
    modulesDirectories: ['src', 'node_modules', 'public'],
  },
  plugins: [
    new Clean([path.resolve(BUILD_PATH, 'shared.*.js')]),
    new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: path.resolve(__dirname) }),
    new webpack.DllPlugin({
      path: path.resolve(MANIFESTS_PATH, '[name]-manifest.json'),
      name: '__[name]',
    }),
  ],
  recordsPath: path.resolve(ROOT_PATH, 'webpack/shared-module-ids.json'),
};

module.exports = webpackConfig;

