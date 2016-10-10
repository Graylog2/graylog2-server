const webpack = require('webpack');
const glob = require('glob');
const path = require('path');
const merge = require('webpack-merge');

const ROOT_PATH = path.resolve(__dirname);
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');
const VENDOR_MANIFEST_PATH = path.resolve(MANIFESTS_PATH, 'vendor-manifest.json');
const VENDOR_MANIFEST = require(VENDOR_MANIFEST_PATH);

const pluginPrefix = '../../graylog-plugin-*/**/';
const pluginConfigPattern = pluginPrefix + 'webpack.config.js';

const pluginConfigs = process.env.disable_plugins == 'true' ? [] : glob.sync(pluginConfigPattern);

process.env.web_src_path = path.resolve(__dirname);

const webpackConfig = require(path.resolve(__dirname, './webpack.config.js'));

pluginConfigs.forEach(function(pluginConfig) {
  const pluginName = pluginConfig.split('/')[2];
  const pluginDir = path.resolve(pluginConfig, '../src/web');
  webpackConfig.entry[pluginName] = pluginDir;
  webpackConfig.resolve.modulesDirectories.unshift(pluginDir);
  webpackConfig.plugins.unshift(new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: path.resolve(pluginDir, '../..') }));
});

module.exports = webpackConfig;
