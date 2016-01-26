var karmaSharedConfig = require('./karma.shared');
var merge = require('webpack-merge');

module.exports = function(config) {
  karmaSharedConfig(config);
  config.set({
    autoWatch: false,
    browsers: ['PhantomJS'],
    singleRun: true,
  });
};
