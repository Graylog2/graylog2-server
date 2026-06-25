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
const AssetsPlugin = require('assets-webpack-plugin');
const { merge } = require('webpack-merge');
const TerserPlugin = require('terser-webpack-plugin');
const { CycloneDxWebpackPlugin } = require('@cyclonedx/webpack-plugin');

const ROOT_PATH = path.resolve(__dirname);
const BUILD_PATH = path.resolve(ROOT_PATH, 'target/web/build');
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');

const vendorModules = require('./vendor.modules');

const TARGET = process.env.npm_lifecycle_event || 'build';
process.env.BABEL_ENV = TARGET;

export const DEFAULT_API_URL = 'http://localhost:9000';

// eslint-disable-next-line no-console
console.error('Building vendor bundle.');

// Plugin to strip HMR-related entries from the DLL manifest.
// In multi-compiler dev mode, webpack-dev-server injects its client and hot entries
// into all compilers. If these end up in the DLL manifest, the app compiler resolves
// them from the DLL, causing the HMR emitter chain to break (different instances).
class CleanDllManifestPlugin {
  private manifestPath: string;

  constructor(manifestPath: string) {
    this.manifestPath = manifestPath;
  }

  apply(compiler) {
    compiler.hooks.afterEmit.tap('CleanDllManifestPlugin', () => {
      try {
        const manifest = JSON.parse(fs.readFileSync(this.manifestPath, 'utf-8'));
        const cleaned = {};
        let removed = 0;

        for (const [key, value] of Object.entries(manifest.content)) {
          if (key.includes('webpack-dev-server') || key.includes('webpack/hot')) {
            removed++;
          } else {
            cleaned[key] = value;
          }
        }

        if (removed > 0) {
          manifest.content = cleaned;
          fs.writeFileSync(this.manifestPath, JSON.stringify(manifest));
        }
      } catch (e) {
        // Manifest may not exist on first build
      }
    });
  }
}

const vendorManifestPath = path.resolve(MANIFESTS_PATH, 'vendor-manifest.json');

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
    new CleanDllManifestPlugin(vendorManifestPath),
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

if (TARGET.startsWith('build')) {
  defaultExport = merge(webpackConfig, {
    mode: 'production',
    optimization: {
      concatenateModules: false,
      sideEffects: false,
      minimizer: [
        new TerserPlugin({
          terserOptions: {
            compress: true,
            mangle: true,
          },
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
