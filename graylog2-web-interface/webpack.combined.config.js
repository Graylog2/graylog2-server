const glob = require('glob');
const path = require('path');
const merge = require('webpack-merge');

const pluginConfigs = glob.sync('./plugins/*/webpack.config.js');

module.exports = ['./webpack.config.js']
  .concat(pluginConfigs)
  .map(function(config) { return require(path.resolve(__dirname, config)); })
  .reduce(function(config1, config2) {
    return merge.smart(config2, config1);
  });

