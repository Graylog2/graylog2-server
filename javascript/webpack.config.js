// webpack.config.js
var webpackConfig = require("./webpack.shared.config");
var Html = require('html-webpack-plugin');
var webpack = require("webpack");

webpackConfig.output.filename = "app.[hash].js";

webpackConfig.plugins.push(
  new Html({
    filename: '../../../app/views/build/scripts.scala.html',
    template: 'templates/scripts.scala.html.template'
  }),
  new webpack.optimize.UglifyJsPlugin({
    minimize: true,
    sourceMap: false,
    compress: {
      warnings: false
    }
  }),
  new webpack.optimize.DedupePlugin(),
  new webpack.optimize.OccurenceOrderPlugin()
  );

module.exports = webpackConfig;