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
const fs = require('fs');
const path = require('path');

const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const merge = require('webpack-merge');

const supportedBrowsers = require('./supportedBrowsers');
const core = require('./webpack/core');

const ROOT_PATH = path.resolve(__dirname);
const APP_PATH = path.resolve(ROOT_PATH, 'src');
const BUILD_PATH = path.resolve(ROOT_PATH, 'target/web/build');
const TARGET = process.env.npm_lifecycle_event || 'build';
process.env.BABEL_ENV = TARGET;

// eslint-disable-next-line import/no-dynamic-require
const BOOTSTRAPVARS = require(path.resolve(ROOT_PATH, 'public', 'stylesheets', 'bootstrap-config.json')).vars;
const coreConfig = core.config(TARGET, APP_PATH, ROOT_PATH, ROOT_PATH, supportedBrowsers);

const webpackConfig = merge.smart(coreConfig, {
  name: 'app',
  dependencies: ['vendor'],
  entry: {
    app: APP_PATH,
    polyfill: [path.resolve(APP_PATH, 'polyfill.js')],
  },
  module: {
    rules: [
      {
        test: /bootstrap\.less$/,
        use: [
          {
            loader: 'style-loader',
            options: {
              // implementation to insert at the top of the head tag: https://github.com/webpack-contrib/style-loader#function
              insert: function insertAtTop(element) {
                const parent = document.querySelector('head');
                // @ts-ignore
                const lastInsertedElement = window._lastElementInsertedByStyleLoader;

                if (!lastInsertedElement) {
                  parent.insertBefore(element, parent.firstChild);
                } else if (lastInsertedElement.nextSibling) {
                  parent.insertBefore(element, lastInsertedElement.nextSibling);
                } else {
                  parent.appendChild(element);
                }

                // @ts-ignore
                window._lastElementInsertedByStyleLoader = element;
              },
            },
          },
          'css-loader',
          {
            loader: 'less-loader',
            options: {
              lessOptions: {
                modifyVars: BOOTSTRAPVARS,
              },
            },
          },
        ],
      },
      { test: /\.less$/, use: ['style-loader', 'css-loader', 'less-loader'], exclude: /bootstrap\.less$/ },
    ],
  },
  plugins: [
    new HtmlWebpackPlugin({
      filename: 'module.json',
      inject: false,
      template: path.resolve(ROOT_PATH, 'templates/module.json.template'),
      excludeChunks: ['config'],
      chunksSortMode: core.sortChunks,
    }),
    new HtmlWebpackPlugin({
      title: 'Graylog',
      favicon: path.resolve(ROOT_PATH, 'public/images/favicon.png'),
      filename: 'index.html',
      inject: false,
      template: path.resolve(ROOT_PATH, 'templates/index.html.template'),
      templateParameters: {
        vendorModule: () => JSON.parse(fs.readFileSync(path.resolve(BUILD_PATH, 'vendor-module.json'), 'utf8')),
        pluginNames: () => global.pluginNames,
      },
      chunksSortMode: core.sortChunks,
    }),
  ],
});

if (TARGET === 'start') {
  // eslint-disable-next-line no-console
  console.error('Running in development (no HMR) mode');

  module.exports = merge(webpackConfig, {
    plugins: [
      new webpack.DefinePlugin({
        IS_CLOUD: process.env.IS_CLOUD,
      }),
    ],
  });
}

if (TARGET.startsWith('build')) {
  // eslint-disable-next-line no-console
  console.error('Running in production mode');
  process.env.NODE_ENV = 'production';
}

if (Object.keys(module.exports).length === 0) {
  module.exports = webpackConfig;
}
