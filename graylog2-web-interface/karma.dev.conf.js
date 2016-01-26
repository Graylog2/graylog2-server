var karmaSharedConfig = require('./karma.shared');

module.exports = function (config) {
  karmaSharedConfig(config);
  config.set({
    autoWatch: true,
    browsers: ['Chrome'],
    singleRun: false,
  });
};
