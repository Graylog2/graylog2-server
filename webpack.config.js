const PluginWebpackConfig = require('graylog-web-plugin').PluginWebpackConfig;
const loadBuildConfig = require('graylog-web-plugin').loadBuildConfig;
const path = require('path');

module.exports = new PluginWebpackConfig('org.graylog.plugins.pipelineprocessor.PipelineProcessorPlugin', loadBuildConfig(path.resolve(__dirname, './build.config')), {
  loaders: [
    { test: /\.ts$/, loader: 'babel-loader!ts-loader', exclude: /node_modules|\.node_cache/ },
  ],
  resolve: {
    extensions: ['.ts'],
  }
});