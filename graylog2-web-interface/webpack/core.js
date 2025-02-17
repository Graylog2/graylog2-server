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
const { EsbuildPlugin } = require('esbuild-loader');
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin');
const { CycloneDxWebpackPlugin } = require('@cyclonedx/webpack-plugin');

const UniqueChunkIdPlugin = require('./UniqueChunkIdPlugin');

const getCssLoaderOptions = (target) => {
  // Development
  if (target === 'start') {
    return {
      modules: {
        localIdentName: '[name]__[local]--[hash:base64:5]',
        mode: 'global',
      },
    };
  }

  return {
    modules: {
      mode: 'global',
    },
  };
};

const sortChunks = (c1, c2) => {
  // Render the polyfill chunk first
  if (c1 === 'polyfill') {
    return -1;
  }

  if (c2 === 'polyfill') {
    return 1;
  }

  if (c1 === 'app') {
    return 1;
  }

  if (c2 === 'app') {
    return -1;
  }

  return 0;
};

const rules = (target, supportedBrowsers) => [
  {
    test: /\.[jt]s(x)?$/,
    use: {
      loader: 'esbuild-loader',
      options: {
        target: supportedBrowsers,
      },
    },
    exclude: /node_modules\/(?!(@react-hook|uuid|@?react-leaflet|graylog-web-plugin))|\.node_cache/,
  },
  {
    test: /\.(svg)(\?.+)?$/,
    type: 'asset/resource',
  },
  {
    test: /\.(woff(2)?|ttf|eot)(\?.+)?$/,
    type: 'asset/resource',
  },
  {
    test: /\.(png|gif|jpg|jpeg)(\?.+)?$/,
    type: 'asset',
  },
  {
    test: /\.css$/,
    exclude: /(\.lazy|leaflet)\.css$/,
    use: [
      'style-loader',
      {
        loader: 'css-loader',
        options: getCssLoaderOptions(target),
      },
    ],
  },
  {
    test: /(\.lazy|leaflet)\.css$/,
    use: [
      { loader: 'style-loader', options: { injectType: 'lazyStyleTag' } },
      {
        loader: 'css-loader',
        options: getCssLoaderOptions(target),
      },
    ],
  },
];

const config = (target, appPath, rootPath, webInterfaceRoot, supportedBrowsers) => {
  const MANIFESTS_PATH = path.resolve(webInterfaceRoot, 'manifests');
  const VENDOR_MANIFEST_PATH = path.resolve(MANIFESTS_PATH, 'vendor-manifest.json');
  const BUILD_PATH = path.resolve(rootPath, 'target/web/build');

  const baseConfig = {
    output: {
      path: BUILD_PATH,
      filename: '[name].[chunkhash].js',
    },
    resolve: {
      // you can now require('file') instead of require('file.coffee')
      extensions: ['.js', '.json', '.jsx', '.ts', '.tsx'],
      modules: [appPath, path.resolve(rootPath, 'node_modules'), path.resolve(rootPath, 'public')],
      alias: {
        '@graylog/server-api': path.resolve(webInterfaceRoot, 'target', 'api'),
      },
    },
    module: {
      rules: rules(target, supportedBrowsers),
    },
    resolveLoader: { modules: [path.join(webInterfaceRoot, 'node_modules')] },
    devtool: 'source-map',
    plugins: [
      new webpack.ProvidePlugin({
        Buffer: ['buffer', 'Buffer'],
      }),
      new UniqueChunkIdPlugin(),
      new webpack.ids.HashedModuleIdsPlugin({
        hashFunction: 'sha256',
        hashDigestLength: 8,
      }),
      new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST_PATH, context: webInterfaceRoot }),
      new webpack.DefinePlugin({
        FEATURES: JSON.stringify(process.env.FEATURES),
      }),
    ],
  };

  if (target === 'start') {
    return merge(baseConfig, {
      mode: 'development',
      devtool: 'cheap-module-source-map',
      output: {
        filename: '[name].js',
        publicPath: '/',
      },
      plugins: [
        new webpack.DefinePlugin({
          DEVELOPMENT: true,
        }),
        new ForkTsCheckerWebpackPlugin({
          typescript: {
            memoryLimit: 4096,
          },
        }),
      ],
    });
  }

  if (target.startsWith('build')) {
    return merge(baseConfig, {
      mode: 'production',
      optimization: {
        splitChunks: {
          chunks: 'all',
          minSize: 20000,
          minRemainingSize: 0,
          minChunks: 1,
          maxAsyncRequests: 30,
          maxInitialRequests: 30,
          enforceSizeThreshold: 50000,
          usedExports: true,
          cacheGroups: {
            defaultVendors: {
              test: /[\\/]node_modules[\\/]/,
              priority: -10,
              reuseExistingChunk: true,
            },
            default: {
              minChunks: 2,
              priority: -20,
              reuseExistingChunk: true,
            },
          },
        },
        moduleIds: 'deterministic',
        minimizer: [new EsbuildPlugin({
          target: supportedBrowsers,
        })],
      },
      plugins: [
        new webpack.DefinePlugin({
          'process.env.NODE_ENV': JSON.stringify('production'),
        }),
        // Create SBOM files for graylog-server frontend dependencies.
        new CycloneDxWebpackPlugin({
          specVersion: '1.5',
          rootComponentAutodetect: false,
          rootComponentType: 'application',
          rootComponentName: 'graylog-server',
          outputLocation: '../cyclonedx-core',
          includeWellknown: false,
        }),
      ],
    });
  }

  return baseConfig;
};

module.exports = { rules, config, sortChunks };
