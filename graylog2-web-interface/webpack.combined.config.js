const glob = require('glob');
const path = require('path');
const merge = require('webpack-merge');

// Regular plugins
const pluginPattern1 = '../../graylog-plugin-*/webpack.config.js';
// Plugins where the web part is in a subdir
const pluginPattern2 = '../../graylog-plugin-*/*/webpack.config.js';

const pluginConfigs = process.env.disable_plugins == 'true' ? [] : glob.sync(pluginPattern1).concat(glob.sync(pluginPattern2));

process.env.web_src_path = path.resolve(__dirname);

module.exports = ['./webpack.config.js']
  .concat(pluginConfigs)
  .map(function(config) { return require(path.resolve(__dirname, config)); })
  .reduce(function(config1, config2) {
    return merge.smart(config2, config1);
  });
