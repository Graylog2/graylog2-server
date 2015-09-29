// webpack.config.js
var webpackConfig = require('./webpack.shared.config');
var webpack = require('webpack');

webpackConfig.output.filename = '[name].[hash].js';

webpackConfig.plugins.push(
  new webpack.optimize.UglifyJsPlugin({
    minimize: true,
    sourceMap: false,
    compress: {
      warnings: false,
    },
  }),
  new webpack.optimize.DedupePlugin(),
  new webpack.optimize.OccurenceOrderPlugin()
  );

module.exports = webpackConfig;
