// webpack.hmr.config.js
var webpackConfig = require("./webpack.shared.config");
var webpack = require("webpack");

webpackConfig.entry = ['webpack/hot/dev-server', 'webpack/hot/only-dev-server', './src/mount.jsx'];
webpackConfig.output.publicPath = "http://localhost:8080/assets/javascripts/build/";
webpackConfig.plugins.push(new webpack.HotModuleReplacementPlugin());
webpackConfig.plugins.push(new webpack.NoErrorsPlugin());

module.exports = webpackConfig;
