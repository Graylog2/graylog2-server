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
const merge = require('webpack-merge');
const { EsbuildPlugin } = require('esbuild-loader');
const { CycloneDxWebpackPlugin } = require('@cyclonedx/webpack-plugin');

const ROOT_PATH = path.resolve(__dirname);
const BUILD_PATH = path.resolve(ROOT_PATH, 'target/web/build');
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');

const vendorModules = require('./vendor.modules');
const supportedBrowsers = require('./supportedBrowsers');

const TARGET = process.env.npm_lifecycle_event || 'build';
process.env.BABEL_ENV = TARGET;

export const DEFAULT_API_URL = 'http://localhost:9000';
const apiUrl = process.env.GRAYLOG_API_URL ?? DEFAULT_API_URL;

// eslint-disable-next-line no-console
console.error('Building vendor bundle.');

const webpackConfig = {
  mode: 'development',
  name: 'vendor',
  entry: {
    vendor: vendorModules,
  },
  output: {
    publicPath: '',
    path: BUILD_PATH,
    filename: '[name].js',
    library: '__[name]',
    clean: {
      keep: /vendor-module\.json/,
    },
  },
  plugins: [
    new webpack.DllPlugin({
      path: path.resolve(MANIFESTS_PATH, '[name]-manifest.json'),
      name: '__[name]',
    }),
    new AssetsPlugin({
      filename: 'vendor-module.json',
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
  recordsPath: path.resolve(ROOT_PATH, 'webpack/vendor-module-ids.json'),
};

// eslint-disable-next-line import/no-mutable-exports
let defaultExport = webpackConfig;

if (TARGET === 'start') {
  defaultExport = merge(webpackConfig, {
    devServer: {
      hot: false,
      liveReload: true,
      compress: true,
      historyApiFallback: {
        disableDotRule: true,
      },
      proxy: [{
        context: ['/api', '/config.js'],
        target: apiUrl,
      }],
    },
  });
}

if (TARGET.startsWith('build')) {
  defaultExport = merge(webpackConfig, {
    mode: 'production',
    optimization: {
      concatenateModules: false,
      sideEffects: false,
      minimizer: [new EsbuildPlugin({
        format: 'cjs',
        target: supportedBrowsers,
      })],
    },
    plugins: [
      new webpack.DefinePlugin({
        'process.env.NODE_ENV': JSON.stringify('production'),
      }),
      new webpack.LoaderOptionsPlugin({
        minimize: true,
      }),
      // Create SBOM files for graylog-server frontend dependencies.
      new CycloneDxWebpackPlugin({
        specVersion: '1.5',
        rootComponentAutodetect: false,
        rootComponentType: 'application',
        rootComponentName: 'graylog-server',
        outputLocation: '../cyclonedx-vendor',
        includeWellknown: false,
      }),
    ],
    output: {
      filename: '[name].[chunkhash].js',
    },
  });
}

export default defaultExport;
