const glob = require('glob');
const path = require('path');
const merge = require('webpack-merge');

const pluginConfigs = process.env.disable_plugins == 'true' ? [] : glob.sync('../../graylog-plugin-*/webpack.config.js');

process.env.web_src_path = path.resolve(__dirname);

module.exports = ['./webpack.config.js']
  .concat(pluginConfigs)
  .map(function(config) { return require(path.resolve(__dirname, config)); })
  .reduce(function(config1, config2) {
    return merge.smart(config2, config1);
  });
