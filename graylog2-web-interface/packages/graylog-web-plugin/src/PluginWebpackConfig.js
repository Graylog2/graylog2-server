/* eslint-disable global-require,import/no-dynamic-require,no-param-reassign */

const webpack = require('webpack');
const merge = require('webpack-merge');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const defaultRootPath = path.resolve(module.parent.parent.filename, '../');
const defaultOptions = {
  root_path: defaultRootPath,
  entry_path: path.resolve(defaultRootPath, 'src/web/index.jsx'),
  build_path: path.resolve(defaultRootPath, 'target/web/build'),
};

function getPluginFullName(fqcn) {
  return `plugin.${fqcn}`;
}

function prefixAssetPaths(fqcn, config) {
  const rules = (config.module || {}).rules || [];

  rules
    .filter((rule) => rule.loader === 'file-loader') // Only modify file-loader options!
    .forEach((rule) => {
      if (!rule.options) {
        rule.options = {};
      }

      // For file-loader loaders in plugins the default publicPath of
      // "/assets" doesn't work so we need to make sure we set it to
      // the plugin's asset path.
      rule.options.publicPath = `/assets/plugin/${fqcn}`;
    });
}

function PluginWebpackConfig(fqcn, _options, additionalConfig) {
  const options = merge(defaultOptions, _options);
  const VENDOR_MANIFEST = require(path.resolve(_options.web_src_path, 'manifests', 'vendor-manifest.json'));

  const plugins = [
    new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: options.root_path }),
    new HtmlWebpackPlugin({ filename: `${getPluginFullName(fqcn)}.module.json`, inject: false, template: path.resolve(_options.web_src_path, 'templates', 'module.json.template') }),
  ];

  const config = merge.smart(
    require(path.resolve(_options.web_src_path, 'webpack.config.js')),
    {
      output: {
        path: options.build_path,
      },
      plugins: plugins,
      resolve: {
        modules: [path.resolve(options.entry_path, '..')],
      },
    },
  );

  // Modify asset paths AFTER we merged the configs
  prefixAssetPaths(fqcn, config);

  const entry = {};
  entry[getPluginFullName(fqcn)] = options.entry_path;

  config.entry = entry;

  if (additionalConfig) {
    return merge.smart(config, additionalConfig);
  }

  return config;
}

module.exports = PluginWebpackConfig;
