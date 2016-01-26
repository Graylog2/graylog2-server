var karmaSharedConfig = require('./karma.shared');
var merge = require('webpack-merge');

webpackSharedConfig.externals = [];

module.exports = function (config) {
  karmaSharedConfig(config);
  config.set({
    autoWatch: true,
    browsers: ['Chrome'],
    singleRun: false,
  });
};
