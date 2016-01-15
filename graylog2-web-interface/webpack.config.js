// webpack.config.js
const webpack = require('webpack');
const path = require('path');
const Clean = require('clean-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const merge = require('webpack-merge');

const ROOT_PATH = path.resolve(__dirname);
const APP_PATH = path.resolve(ROOT_PATH, 'src');
const BUILD_PATH = path.resolve(ROOT_PATH, 'build');
const VENDOR_MANIFEST_PATH = path.resolve(BUILD_PATH, 'vendor-manifest.json');
const VENDOR_MANIFEST = require(VENDOR_MANIFEST_PATH);
const SHARED_MANIFEST_PATH = path.resolve(BUILD_PATH, 'shared-manifest.json');
const SHARED_MANIFEST = require(SHARED_MANIFEST_PATH);
const TARGET = process.env.npm_lifecycle_event;
process.env.BABEL_ENV = TARGET;

const webpackConfig = {
  entry: {
    app: APP_PATH,
    config: 'config.js',
  },
  output: {
    path: BUILD_PATH,
    filename: '[name].[hash].js',
    publicPath: '/',
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
    modulesDirectories: [APP_PATH, 'node_modules', path.resolve(ROOT_PATH, 'public')],
  },
  eslint: {
    configFile: '.eslintrc',
  },
  devtool: 'eval',
  plugins: [
    new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: ROOT_PATH }),
    new webpack.DllReferencePlugin({ manifest: SHARED_MANIFEST, context: ROOT_PATH }),
    new HtmlWebpackPlugin({title: 'Graylog', favicon: 'public/images/favicon.png', template: 'templates/index.html.template'}),
    new HtmlWebpackPlugin({filename: 'module.json', template: 'templates/module.json.template', excludeChunks: ['config']}),
  ],
};

const commonConfigs = {
  module: {
    loaders: [
      { test: /pages\/.+\.jsx$/, loader: 'react-proxy', exclude: /node_modules|\.node_cache/ },
    ],
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin('config', 'config.js', ['config']),
  ],
};

if (TARGET === 'start') {
  console.log('Running in development mode');
  module.exports = merge(webpackConfig, {
    devtool: 'eval',
    devServer: {
      historyApiFallback: true,
      hot: true,
      inline: true,
      progress: true,
    },
    plugins: [
      new webpack.HotModuleReplacementPlugin(),
    ],
  });
}

if (TARGET === 'build') {
  console.log('Running in production mode');
  module.exports = merge(webpackConfig, {
    plugins: [
      new Clean([BUILD_PATH]),
      new webpack.optimize.UglifyJsPlugin({
        minimize: true,
        sourceMap: false,
        compress: {
          warnings: false,
        },
      }),
      new webpack.optimize.DedupePlugin(),
      new webpack.optimize.OccurenceOrderPlugin(),
    ],
  });
}

if (TARGET === 'test') {
  console.log('Running test/ci mode');
  module.exports = merge(webpackConfig, {
    module: {
      preLoaders: [
        { test: /\.js(x)?$/, loader: 'eslint-loader', exclude: /node_modules|public\/javascripts/ }
      ],
    },
  });
}

if (TARGET === 'start' || TARGET === 'build') {
  module.exports = merge(module.exports, commonConfigs);
}

if (Object.keys(module.exports).length === 0) {
  console.log('Running in default mode');
  module.exports = webpackConfig;
}
