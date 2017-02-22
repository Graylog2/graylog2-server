const webpack = require('webpack');
const merge = require('webpack-merge');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const defaultRootPath = path.resolve(module.parent.parent.filename, '../');
const defaultOptions = {
  root_path: defaultRootPath,
  entry_path: path.resolve(defaultRootPath, 'src/web/index.jsx'),
  build_path: path.resolve(defaultRootPath, 'build'),
};

function getPluginFullName(fqcn) {
  return `plugin.${fqcn}`;
}

function PluginWebpackConfig(fqcn, _options, additionalConfig) {
  const options = merge(defaultOptions, _options);
  const VENDOR_MANIFEST = require(path.resolve(_options.web_src_path, 'manifests', 'vendor-manifest.json'));

  const plugins = [
    new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: options.root_path }),
    new HtmlWebpackPlugin({ filename: getPluginFullName(fqcn) + '.module.json', inject: false, template: path.resolve(_options.web_src_path, 'templates', 'module.json.template') }),
  ];

  const config = merge.smart(
    require(path.resolve(_options.web_src_path, 'webpack.config.js')),
    {
      output: {
        path: options.build_path,
      },
      plugins: plugins,
      resolve: {
        modules: [ path.resolve(options.entry_path, '..') ],
      },
    }
  );

  const entry = {};
  entry[getPluginFullName(fqcn)] = options.entry_path;

  config.entry = entry;

  if (additionalConfig) {
    return merge.smart(config, additionalConfig);
  }

  return config;
}

module.exports = PluginWebpackConfig;
