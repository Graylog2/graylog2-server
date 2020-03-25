const fs = require('fs');
const webpack = require('webpack');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const merge = require('webpack-merge');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin');
const UniqueChunkIdPlugin = require('./webpack/UniqueChunkIdPlugin');

const ROOT_PATH = path.resolve(__dirname);
const APP_PATH = path.resolve(ROOT_PATH, 'src');
const BUILD_PATH = path.resolve(ROOT_PATH, 'target/web/build');
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');
const VENDOR_MANIFEST_PATH = path.resolve(MANIFESTS_PATH, 'vendor-manifest.json');
const TARGET = process.env.npm_lifecycle_event;
process.env.BABEL_ENV = TARGET;

const BABELRC = path.resolve(ROOT_PATH, 'babel.config.js');
const BABELOPTIONS = {
  cacheDirectory: 'target/web/cache',
  extends: BABELRC,
};

const BABELLOADER = { loader: 'babel-loader', options: BABELOPTIONS };

// eslint-disable-next-line import/no-dynamic-require
const BOOTSTRAPVARS = require(path.resolve(ROOT_PATH, 'public', 'stylesheets', 'bootstrap-config.json')).vars;

const getCssLoaderOptions = () => {
  // Development
  if (TARGET === 'start') {
    return {
      localIdentName: '[name]__[local]--[hash:base64:5]',
    };
  }
  return {};
};

const webpackConfig = {
  name: 'app',
  dependencies: ['vendor'],
  entry: {
    app: APP_PATH,
    builtins: [path.resolve(APP_PATH, 'injection', 'builtins.js')],
    polyfill: ['@babel/polyfill'],
  },
  output: {
    path: BUILD_PATH,
    filename: '[name].[hash].js',
  },
  module: {
    rules: [
      { test: /\.js(x)?$/, use: BABELLOADER, exclude: /node_modules|\.node_cache/ },
      { test: /\.ts$/, use: [BABELLOADER, { loader: 'ts-loader' }], exclude: /node_modules|\.node_cache/ },
      { test: /\.(png|gif|jpg)$/, use: 'url-loader' },
      { test: /\.(woff(2)?|ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/, loader: 'file-loader' },
      {
        test: /bootstrap\.less$/,
        use: [
          {
            loader: 'style-loader',
            options: {
              insertAt: 'top',
            },
          },
          'css-loader',
          {
            loader: 'less-loader',
            options: {
              modifyVars: BOOTSTRAPVARS,
            },
          },
        ],
      },
      { test: /\.less$/, use: ['style-loader', 'css-loader', 'less-loader'], exclude: /bootstrap\.less$/ },
      {
        test: /\.css$/,
        use: [
          'style-loader',
          {
            loader: 'css-loader',
            options: getCssLoaderOptions(),
          },
        ],
      },
    ],
  },
  resolve: {
    // you can now require('file') instead of require('file.coffee')
    extensions: ['.js', '.json', '.jsx', '.ts'],
    modules: [APP_PATH, 'node_modules', path.resolve(ROOT_PATH, 'public')],
    alias: {
      theme: path.resolve(APP_PATH, 'theme'),
    },
  },
  resolveLoader: { modules: [path.join(ROOT_PATH, 'node_modules')], moduleExtensions: ['-loader'] },
  devtool: 'source-map',
  plugins: [
    new UniqueChunkIdPlugin(),
    new webpack.HashedModuleIdsPlugin({
      hashFunction: 'sha256',
      hashDigestLength: 8,
    }),
    new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST_PATH, context: ROOT_PATH }),
    new HtmlWebpackPlugin({
      title: 'Graylog',
      favicon: path.resolve(ROOT_PATH, 'public/images/favicon.png'),
      filename: 'index.html',
      inject: false,
      template: path.resolve(ROOT_PATH, 'templates/index.html.template'),
      vendorModule: () => JSON.parse(fs.readFileSync(path.resolve(BUILD_PATH, 'vendor-module.json'), 'utf8')),
      chunksSortMode: (c1, c2) => {
        // Render the polyfill chunk first
        if (c1.names[0] === 'polyfill') {
          return -1;
        }
        if (c2.names[0] === 'polyfill') {
          return 1;
        }
        if (c1.names[0] === 'builtins') {
          return -1;
        }
        if (c2.names[0] === 'builtins') {
          return 1;
        }
        if (c1.names[0] === 'app') {
          return 1;
        }
        if (c2.names[0] === 'app') {
          return -1;
        }
        return c2.id - c1.id;
      },
    }),
    new HtmlWebpackPlugin({ filename: 'module.json', inject: false, template: path.resolve(ROOT_PATH, 'templates/module.json.template'), excludeChunks: ['config'] }),
    new webpack.DefinePlugin({
      FEATURES: JSON.stringify(process.env.FEATURES),
    }),
  ],
};

if (TARGET === 'start') {
  // eslint-disable-next-line no-console
  console.error('Running in development (no HMR) mode');
  module.exports = merge(webpackConfig, {
    mode: 'development',
    devtool: 'cheap-module-source-map',
    output: {
      path: BUILD_PATH,
      filename: '[name].js',
      publicPath: '/',
    },
    plugins: [
      new webpack.DefinePlugin({
        DEVELOPMENT: true,
        GRAYLOG_HTTP_PUBLISH_URI: JSON.stringify(process.env.GRAYLOG_HTTP_PUBLISH_URI),
      }),
      new CopyWebpackPlugin([{ from: 'config.js' }]),
      new webpack.HotModuleReplacementPlugin(),
    ],
  });
}

if (TARGET === 'build') {
  // eslint-disable-next-line no-console
  console.error('Running in production mode');
  process.env.NODE_ENV = 'production';
  module.exports = merge(webpackConfig, {
    mode: 'production',
    optimization: {
      minimizer: [new TerserPlugin({
        sourceMap: true,
        terserOptions: {
          compress: {
            warnings: false,
          },
          mangle: {
            reserved: ['$super', '$', 'exports', 'require'],
          },
        },
      })],
    },
    plugins: [
      new webpack.DefinePlugin({
        'process.env.NODE_ENV': JSON.stringify('production'),
      }),
      // Looking at https://webpack.js.org/plugins/loader-options-plugin, this plugin seems to not
      // be needed any longer. We should try deleting it next time we clean up this configuration.
      new webpack.LoaderOptionsPlugin({
        minimize: true,
      }),
    ],
  });
}

if (TARGET === 'test') {
  // eslint-disable-next-line no-console
  console.error('Running test/ci mode');
  module.exports = merge(webpackConfig, {
    module: {
      rules: [
        { test: /\.js(x)?$/, enforce: 'pre', loader: 'eslint-loader', exclude: /node_modules|public\/javascripts/ },
      ],
    },
  });
}

if (Object.keys(module.exports).length === 0) {
  module.exports = webpackConfig;
}
