var merge = require('webpack-merge');
var webpackSharedConfig = merge(require('./webpack.config'), {
  module: {
    noParse: [
      /node_modules\/sinon\//,
    ]
  },
  resolve: {
    alias: {
      'sinon': 'sinon/pkg/sinon'
    }
  },
  externals: {
    'jsdom': 'window',
    'cheerio': 'window',
    'react/lib/ExecutionEnvironment': true,
    'react/lib/ReactContext': true,
  }
});

module.exports = function(config) {
  config.set({

    basePath: '',

    frameworks: ['jasmine'],

    files: [
      'config.js',
      'build/vendor.js',
      'test/shim/es5-shim.js',
      'test/shim/server-side-global-vars.js',
      'test/src/*.js',
      'test/src/**/*.js',
      'test/src/*.jsx',
      'test/src/**/*.jsx',
    ],

    preprocessors: {
      'test/src/*.js': ['webpack'],
      'test/src/**/*.js': ['webpack'],
      'test/src/*.jsx': ['webpack'],
      'test/src/**/*.jsx': ['webpack'],
    },

    reporters: ['progress'],

    port: 9876,

    colors: true,

    logLevel: config.LOG_INFO,

    captureTimeout: 60000,

    webpack: webpackSharedConfig
  });
};
