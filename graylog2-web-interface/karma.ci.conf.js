var karmaSharedConfig = require('./karma.shared');

module.exports = function(config) {
  karmaSharedConfig(config);
  config.set({
    autoWatch: false,
    browsers: ['PhantomJS'],
    singleRun: true,
  });
};
