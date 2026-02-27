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
const AssetsPlugin = require('assets-webpack-plugin');
const { merge } = require('webpack-merge');
const { EsbuildPlugin } = require('esbuild-loader');

const core = require('./webpack/core');
const commonsModules = require('./commons.modules');
const supportedBrowsers = require('./supportedBrowsers');

const ROOT_PATH = path.resolve(__dirname);
const APP_PATH = path.resolve(ROOT_PATH, 'src');
const BUILD_PATH = path.resolve(ROOT_PATH, 'target/web/build');
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');
const VENDOR_MANIFEST_PATH = path.resolve(MANIFESTS_PATH, 'vendor-manifest.json');

const TARGET = process.env.npm_lifecycle_event || 'build';
process.env.BABEL_ENV = TARGET;

// eslint-disable-next-line no-console
console.error('Building commons bundle.');

const webpackConfig = {
  mode: 'development',
  name: 'commons',
  dependencies: ['vendor'],
  entry: {
    commons: commonsModules,
  },
  output: {
    publicPath: '',
    path: BUILD_PATH,
    filename: '[name].js',
    library: '__[name]',
  },
  resolve: {
    extensions: ['.js', '.json', '.jsx', '.ts', '.tsx'],
    modules: [APP_PATH, path.resolve(ROOT_PATH, 'node_modules'), path.resolve(ROOT_PATH, 'public')],
    alias: {
      '@graylog/server-api': path.resolve(ROOT_PATH, 'target', 'api'),
    },
  },
  module: {
    rules: core.rules(TARGET, supportedBrowsers),
  },
  resolveLoader: { modules: [path.resolve(ROOT_PATH, 'node_modules')] },
  plugins: [
    new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST_PATH, context: ROOT_PATH }),
    new webpack.DllPlugin({
      path: path.resolve(MANIFESTS_PATH, '[name]-manifest.json'),
      name: '__[name]',
      context: ROOT_PATH,
    }),
    new AssetsPlugin({
      filename: 'commons-module.json',
      useCompilerPath: true,
      processOutput(assets) {
        const jsfiles = [];
        const cssfiles = [];
        const chunks = {};

        Object.keys(assets).forEach((chunk) => {
          if (assets[chunk].js) {
            jsfiles.push(assets[chunk].js);
          }

          if (assets[chunk].css) {
            jsfiles.push(assets[chunk].css);
          }

          chunks[chunk] = {
            size: 0,
            entry: assets[chunk].js,
            css: assets[chunk].css || [],
          };
        });

        return JSON.stringify({
          files: {
            js: jsfiles,
            css: cssfiles,
            chunks: chunks,
          },
        });
      },
    }),
  ],
  recordsPath: path.resolve(ROOT_PATH, 'webpack/commons-module-ids.json'),
};

if (TARGET.startsWith('build')) {
  module.exports = merge(webpackConfig, {
    mode: 'production',
    optimization: {
      concatenateModules: false,
      sideEffects: false,
      minimizer: [
        new EsbuildPlugin({
          format: 'cjs',
          target: supportedBrowsers,
        }),
      ],
    },
    plugins: [
      new webpack.DefinePlugin({
        'process.env.NODE_ENV': JSON.stringify('production'),
      }),
      new webpack.LoaderOptionsPlugin({
        minimize: true,
      }),
    ],
    output: {
      filename: '[name].[chunkhash].js',
    },
  });
} else {
  module.exports = webpackConfig;
}
