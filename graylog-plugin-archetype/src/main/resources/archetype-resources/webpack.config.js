const path = require('path');
const { PluginWebpackConfig } = require('graylog-web-plugin');
const { loadBuildConfig } = require('graylog-web-plugin');

// Remember to use the same name here and in `getUniqueId()` in the java MetaData class
module.exports = new PluginWebpackConfig(__dirname, '${package}.${pluginClassName}Plugin', loadBuildConfig(path.resolve(__dirname, './build.config')), {
  // Here goes your additional webpack configuration.
});
