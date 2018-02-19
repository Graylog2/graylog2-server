const webpack = require('webpack');
const glob = require('glob');
const path = require('path');
const fs = require('fs');

const ROOT_PATH = path.resolve(__dirname);
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');
const VENDOR_MANIFEST_PATH = path.resolve(MANIFESTS_PATH, 'vendor-manifest.json');
const TARGET = process.env.npm_lifecycle_event;

const pluginConfigPattern = 'graylog-plugin-*/**/webpack.config.js';
const globCwd = '../..';
const globOptions = {
  ignore: '**/node_modules/**',
  cwd: globCwd,
  nodir: true,
};

const pluginConfigs = process.env.disable_plugins === 'true' ? [] : glob.sync(pluginConfigPattern, globOptions).map(config => `${globCwd}/${config}`);

process.env.web_src_path = path.resolve(__dirname);

// eslint-disable-next-line import/no-dynamic-require
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

pluginConfigs.filter(isNotDependency).forEach((pluginConfig) => {
  const pluginName = getPluginName(pluginConfig);
  const pluginDir = path.resolve(pluginConfig, '../src/web');
  webpackConfig.entry[pluginName] = pluginDir;
  webpackConfig.resolve.modules.unshift(pluginDir);
  webpackConfig.plugins.unshift(new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST_PATH, context: path.resolve(pluginDir, '../..') }));
});

// We need to inject webpack-hot-middleware to all entries, ensuring the app is able to reload on changes.
if (TARGET === 'start') {
  const hmrEntries = {};
  const webpackHotMiddlewareEntry = 'webpack-hot-middleware/client?reload=true';

  Object.keys(webpackConfig.entry).forEach((entryKey) => {
    const entryValue = webpackConfig.entry[entryKey];
    const hmrValue = [webpackHotMiddlewareEntry];
    if (Array.isArray(entryValue)) {
      hmrValue.push(...entryValue);
    } else {
      hmrValue.push(entryValue);
    }
    hmrEntries[entryKey] = hmrValue;
  });

  webpackConfig.entry = hmrEntries;
}

module.exports = webpackConfig;
