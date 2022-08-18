/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
const path = require('path');

const webpack = require('webpack');
const merge = require('webpack-merge');
const HtmlWebpackPlugin = require('html-webpack-plugin');

function getPluginFullName(fqcn) {
  return `plugin.${fqcn}`;
}

function PluginWebpackConfig(defaultRootPath, fqcn, _options, additionalConfig) {
  const defaultOptions = {
    root_path: defaultRootPath,
    entry_path: path.resolve(defaultRootPath, 'src/web/index.jsx'),
    build_path: path.resolve(defaultRootPath, 'target/web/build'),
  };

  const options = merge(defaultOptions, _options);
  // eslint-disable-next-line global-require,import/no-dynamic-require
  const VENDOR_MANIFEST = require(path.resolve(_options.web_src_path, 'manifests', 'vendor-manifest.json'));

  const plugins = [
    new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: options.root_path }),
    new HtmlWebpackPlugin({ filename: `${getPluginFullName(fqcn)}.module.json`, inject: false, template: path.resolve(_options.web_src_path, 'templates', 'module.json.template') }),
  ];

  const config = merge.smart(
    // eslint-disable-next-line global-require,import/no-dynamic-require
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

  const entry = {};
  entry[getPluginFullName(fqcn)] = options.entry_path;

  config.entry = entry;

  if (additionalConfig) {
    return merge.smart(config, additionalConfig);
  }

  return config;
}

module.exports = PluginWebpackConfig;
