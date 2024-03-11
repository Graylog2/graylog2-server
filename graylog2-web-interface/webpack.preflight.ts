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
const HtmlWebpackPlugin = require('html-webpack-plugin');
const merge = require('webpack-merge');
const { EsbuildPlugin } = require('esbuild-loader');
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin');

const { DEFAULT_API_URL } = require('./webpack.vendor');
const supportedBrowsers = require('./supportedBrowsers');
const core = require('./webpack/core');

const ROOT_PATH = path.resolve(__dirname);
const APP_PATH = path.resolve(ROOT_PATH, 'src');
const PREFLIGHT_PATH = path.resolve(ROOT_PATH, 'src/preflight');
const BUILD_PATH = path.resolve(ROOT_PATH, 'target/preflight/build');
const TARGET = process.env.npm_lifecycle_event || 'build';
process.env.BABEL_ENV = TARGET;
const mode = TARGET.startsWith('build') ? 'production' : 'development';

const apiUrl = process.env.GRAYLOG_API_URL ?? DEFAULT_API_URL;

const baseConfig = {
  mode,
  name: 'preflight',
  entry: {
    preflight: PREFLIGHT_PATH,
  },
  output: {
    path: BUILD_PATH,
    filename: '[name].[chunkhash].js',
  },
  resolve: {
    // you can now require('file') instead of require('file.coffee')
    extensions: ['.js', '.json', '.jsx', '.ts', '.tsx'],
    modules: [APP_PATH, path.resolve(ROOT_PATH, 'node_modules'), path.resolve(ROOT_PATH, 'public')],
  },
  module: {
    rules: core.rules(TARGET, supportedBrowsers),
  },
  devtool: 'source-map',
  plugins: [
    new HtmlWebpackPlugin({
      title: 'Graylog Initial Setup',
      favicon: path.resolve(ROOT_PATH, 'public/images/favicon.png'),
      filename: 'index.html',
      inject: true,
      template: path.resolve(ROOT_PATH, 'templates/index.html.preflight.template'),
    }),
  ],
};

let webpackConfig;

if (mode === 'development') {
  webpackConfig = merge(baseConfig, {
    devServer: {
      hot: false,
      liveReload: true,
      compress: true,
      proxy: [{
        context: ['/api'],
        target: apiUrl,
      }],
    },
    devtool: 'cheap-module-source-map',
    output: {
      filename: '[name].js',
      publicPath: '/',
    },
    plugins: [
      new webpack.DefinePlugin({
        DEVELOPMENT: true,
      }),
      new ForkTsCheckerWebpackPlugin(),
    ],
  });
}

if (mode === 'production') {
  webpackConfig = merge(baseConfig, {
    optimization: {
      moduleIds: 'deterministic',
      minimizer: [new EsbuildPlugin({
        target: supportedBrowsers,
      })],
    },
    plugins: [
      new webpack.DefinePlugin({
        'process.env.NODE_ENV': JSON.stringify('production'),
      }),
    ],
  });
}

module.exports = webpackConfig;
