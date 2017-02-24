const PluginWebpackConfig = require('graylog-web-plugin').PluginWebpackConfig;
const loadBuildConfig = require('graylog-web-plugin').loadBuildConfig;
const path = require('path');

module.exports = new PluginWebpackConfig('org.graylog.plugins.enterprise_integration.EnterpriseIntegrationPlugin', loadBuildConfig(path.resolve(__dirname, './build.config')), {
  module: {
    rules: [
      { test: /\.ts$/, use: ['babel-loader', 'ts-loader'], exclude: /node_modules|\.node_cache/ },
    ],
  },
  resolve: {
    extensions: ['.ts'],
  },
});
