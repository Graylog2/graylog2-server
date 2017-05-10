const webpack = require('webpack');
const glob = require('glob');
const path = require('path');
const fs = require('fs');
const merge = require('webpack-merge');

const ROOT_PATH = path.resolve(__dirname);
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');
const VENDOR_MANIFEST_PATH = path.resolve(MANIFESTS_PATH, 'vendor-manifest.json');

const pluginPrefix = '../../graylog-plugin-*/**/';
const pluginConfigPattern = pluginPrefix + 'webpack.config.js';

const pluginConfigs = process.env.disable_plugins === 'true' ? [] : glob.sync(pluginConfigPattern);

process.env.web_src_path = path.resolve(__dirname);

const webpackConfig = require(path.resolve(__dirname, './webpack.config.js'));

function getPluginName(pluginConfig) {
  const packageConfig = path.join(path.dirname(pluginConfig), 'package.json');
  if (fs.existsSync(packageConfig)) {
    // If a package.json file exists (should normally be the case) use the package name for pluginName
    const pkg = JSON.parse(fs.readFileSync(packageConfig, 'utf8'));
    return pkg.name.replace(/\s+/g, '');
  } else {
    // Otherwise just use the directory name of the webpack config file
    return path.basename(path.dirname(pluginConfig));
  }
}

function isNotDependency(pluginConfig) {
  // Avoid including webpack configs of dependencies and built files.
  return !pluginConfig.includes('/target/') && !pluginConfig.includes('/node_modules/');
}

pluginConfigs.filter(isNotDependency).forEach(pluginConfig => {
  const pluginName = getPluginName(pluginConfig);
  const pluginDir = path.resolve(pluginConfig, '../src/web');
  webpackConfig.entry[pluginName] = pluginDir;
  webpackConfig.resolve.modules.unshift(pluginDir);
  webpackConfig.plugins.unshift(new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST_PATH, context: path.resolve(pluginDir, '../..') }));
});

module.exports = webpackConfig;
