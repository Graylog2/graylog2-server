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

const TARGET = process.env.npm_lifecycle_event || 'build';

function getPluginFullName(fqcn) {
  return `plugin.${fqcn}`;
}

function PluginWebpackConfig(defaultRootPath, fqcn, _options, additionalConfig) {
  const defaultOptions = {
    root_path: defaultRootPath,
    src_path: path.resolve(defaultRootPath, 'src/web'),
    entry_path: path.resolve(defaultRootPath, 'src/web/index.jsx'),
    build_path: path.resolve(defaultRootPath, 'target/web/build'),
  };

  const options = merge(defaultOptions, _options);
  /* eslint-disable global-require,import/no-dynamic-require */
  const VENDOR_MANIFEST = require(path.resolve(options.web_src_path, 'manifests', 'vendor-manifest.json'));
  const core = require(path.resolve(options.web_src_path, 'webpack/core'));
  const supportedBrowsers = require(path.resolve(options.web_src_path, 'supportedBrowsers'));
  /* eslint-enable global-require,import/no-dynamic-require */

  const plugins = [
    new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: options.root_path }),
    new HtmlWebpackPlugin({ filename: `${getPluginFullName(fqcn)}.module.json`, inject: false, template: path.resolve(options.web_src_path, 'templates', 'module.json.template') }),
  ];
  const fullPluginName = getPluginFullName(fqcn);

  const config = merge.smart({
    name: fullPluginName,
    dependencies: ['vendor'],
    entry: {
      [fullPluginName]: options.entry_path,
    },
    output: {
      path: options.build_path,
    },
    plugins: plugins,
    resolve: {
      modules: [path.resolve(options.web_src_path, 'src'), path.resolve(options.web_src_path, 'node_modules')],
    },
  },
  core.config(TARGET, options.src_path, options.root_path, options.web_src_path, supportedBrowsers),
  );

  if (additionalConfig) {
    return merge.smart(config, additionalConfig);
  }

  return config;
}

module.exports = PluginWebpackConfig;
