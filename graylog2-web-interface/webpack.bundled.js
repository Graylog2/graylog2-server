const webpackConfig = require('./webpack.combined.config');
const vendorConfig = require('./webpack.vendor');

module.exports = [vendorConfig, webpackConfig];
