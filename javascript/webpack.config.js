// webpack.config.js
var webpackConfig = require("./webpack.dev.config");
var Clean = require('clean-webpack-plugin');
var Html = require('html-webpack-plugin');

webpackConfig.output.filename = "app.[hash].js";
webpackConfig.plugins = [
    new Clean([webpackConfig.output.path]),
    new Html({
        filename: '../../../app/views/build/scripts.scala.html',
        template: 'templates/scripts.scala.html.template'
    })
];

module.exports = webpackConfig;