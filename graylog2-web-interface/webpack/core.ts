const path = require('path');

const webpack = require('webpack');
const merge = require('webpack-merge');
const { EsbuildPlugin } = require('esbuild-loader');
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin');

const UniqueChunkIdPlugin = require('./UniqueChunkIdPlugin');

const getCssLoaderOptions = (target: string) => {
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

export const sortChunks = (c1: string, c2: string) => {
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

export const rules = (target: string, supportedBrowsers: Array<string>) => [
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

export const config = (target: string, appPath: string, rootPath: string, webInterfaceRoot: string, supportedBrowsers: Array<string>) => {
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
        new webpack.HotModuleReplacementPlugin(),
        new ForkTsCheckerWebpackPlugin(),
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
      ],
    });
  }
};
