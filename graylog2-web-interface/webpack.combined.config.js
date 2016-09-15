const webpack = require('webpack');
const glob = require('glob');
const path = require('path');
const merge = require('webpack-merge');

const ROOT_PATH = path.resolve(__dirname);
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');
const VENDOR_MANIFEST_PATH = path.resolve(MANIFESTS_PATH, 'vendor-manifest.json');
const VENDOR_MANIFEST = require(VENDOR_MANIFEST_PATH);

// Regular plugins
const pluginPattern1 = '../../graylog-plugin-*/webpack.config.js';
// Plugins where the web part is in a subdir
const pluginPattern2 = '../../graylog-plugin-*/*/webpack.config.js';

const pluginConfigs = process.env.disable_plugins == 'true' ? [] : glob.sync(pluginPattern1).concat(glob.sync(pluginPattern2));

process.env.web_src_path = path.resolve(__dirname);

const webpackConfig = require(path.resolve(__dirname, './webpack.config.js'));

pluginConfigs.forEach(function(plugin) {
  const pluginName = plugin.split('/')[2];
  const pluginDir = path.resolve(__dirname, '../..', pluginName, 'src/web');
  webpackConfig.entry[pluginName] = pluginDir;
  webpackConfig.resolve.modulesDirectories.unshift(pluginDir);
  webpackConfig.plugins.unshift(new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: path.resolve(pluginDir, '../..') }));
});

module.exports = webpackConfig;
