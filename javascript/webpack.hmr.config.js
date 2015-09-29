// webpack.hmr.config.js
var webpackConfig = require("./webpack.shared.config");
var webpack = require("webpack");

webpackConfig.entry = {
  hotDevServer: 'webpack/hot/dev-server',
  onlyDevServer: 'webpack/hot/only-dev-server',
  app: './src/mount.jsx',
  config: './src/config.js'
};
webpackConfig.output.publicPath = "http://localhost:8080/";
webpackConfig.plugins.push(new webpack.HotModuleReplacementPlugin());
webpackConfig.plugins.push(new webpack.NoErrorsPlugin());

module.exports = webpackConfig;
