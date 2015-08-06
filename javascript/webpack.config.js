// webpack.config.js
var webpackConfig = require("./webpack.shared.config");
var Html = require('html-webpack-plugin');

webpackConfig.output.filename = "app.[hash].js";

webpackConfig.plugins.push(new Html({
        filename: '../../../app/views/build/scripts.scala.html',
        template: 'templates/scripts.scala.html.template'
    }));

module.exports = webpackConfig;