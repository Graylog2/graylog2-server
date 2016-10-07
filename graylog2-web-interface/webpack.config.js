// webpack.config.js
const webpack = require('webpack');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const merge = require('webpack-merge');

const ROOT_PATH = path.resolve(__dirname);
const APP_PATH = path.resolve(ROOT_PATH, 'src');
const BUILD_PATH = path.resolve(ROOT_PATH, 'build');
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');
const VENDOR_MANIFEST_PATH = path.resolve(MANIFESTS_PATH, 'vendor-manifest.json');
const VENDOR_MANIFEST = require(VENDOR_MANIFEST_PATH);
const TARGET = process.env.npm_lifecycle_event;
process.env.BABEL_ENV = TARGET;

const BABELRC = path.resolve(ROOT_PATH, '.babelrc');
const BABELLOADER = 'babel-loader?cacheDirectory&extends=' + BABELRC;

const webpackConfig = {
  entry: {
    app: APP_PATH,
    polyfill: ['babel-polyfill'],
  },
  output: {
    path: BUILD_PATH,
    filename: '[name].[hash].js',
  },
  module: {
    preLoaders: [
      // { test: /\.js(x)?$/, loader: 'eslint-loader', exclude: /node_modules|public\/javascripts/ }
    ],
    loaders: [
      { test: /pages\/.+\.jsx$/, loader: 'react-proxy', exclude: /node_modules|\.node_cache|ServerUnavailablePage/ },
      { test: /\.js(x)?$/, loader: BABELLOADER, exclude: /node_modules|\.node_cache/ },
      { test: /\.json$/, loader: 'json' },
      { test: /\.ts$/, loaders: [BABELLOADER, 'ts'], exclude: /node_modules|\.node_cache/ },
      { test: /\.(woff(2)?|svg|eot|ttf|gif|jpg)(\?.+)?$/, loader: 'file' },
      { test: /\.png$/, loader: 'url' },
      { test: /\.less$/, loaders: ['style', 'css', 'less'] },
      { test: /\.css$/, loaders: ['style', 'css'] },
    ],
  },
  resolve: {
    // you can now require('file') instead of require('file.coffee')
    extensions: ['', '.js', '.json', '.jsx', '.ts'],
    modulesDirectories: [APP_PATH, 'node_modules', path.resolve(ROOT_PATH, 'public')],
  },
  resolveLoader: { root: path.join(ROOT_PATH, 'node_modules') },
  eslint: {
    configFile: '.eslintrc',
  },
  devtool: 'source-map',
  plugins: [
    new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: ROOT_PATH }),
    new HtmlWebpackPlugin({
      title: 'Graylog',
      favicon: path.resolve(ROOT_PATH, 'public/images/favicon.png'),
      filename: 'index.html',
      inject: false,
      template: path.resolve(ROOT_PATH, 'templates/index.html.template'),
      chunksSortMode: (c1, c2) => {
        // Render the polyfill chunk first
        if (c1.names[0] === 'polyfill') {
          return -1;
        }
        if (c2.names[0] === 'polyfill') {
          return 1;
        }
        return c2.id - c1.id;
      },
    }),
    new HtmlWebpackPlugin({ filename: 'module.json', inject: false, template: path.resolve(ROOT_PATH, 'templates/module.json.template'), excludeChunks: ['config'] }),
  ],
};

if (TARGET === 'start') {
  console.log('Running in development mode');
  module.exports = merge(webpackConfig, {
    entry: {
      reacthot: 'react-hot-loader/patch',
    },
    devtool: 'eval',
    devServer: {
      historyApiFallback: true,
      hot: true,
      inline: true,
      progress: true,
      watchOptions: {
        ignored: /node_modules/,
      },
    },
    output: {
      path: BUILD_PATH,
      filename: '[name].js',
      publicPath: '/',
      hotUpdateChunkFilename: "[id].hot-update.js",
      hotUpdateMainFilename: "hot-update.json",
    },
    plugins: [
      new webpack.HotModuleReplacementPlugin(),
      new webpack.DefinePlugin({DEVELOPMENT: true}),
    ],
  });
}

if (TARGET === 'start-nohmr') {
  console.log('Running in development (no HMR) mode');
  module.exports = merge(webpackConfig, {
    devtool: 'eval',
    devServer: {
      historyApiFallback: true,
      hot: false,
      inline: true,
      progress: true,
      watchOptions: {
        ignored: /node_modules/,
      },
    },
    output: {
      path: BUILD_PATH,
      filename: '[name].js',
      publicPath: '/',
    },
    plugins: [
      new webpack.DefinePlugin({DEVELOPMENT: true}),
    ],
  });
}

if (TARGET === 'build') {
  console.log('Running in production mode');
  module.exports = merge(webpackConfig, {
    plugins: [
      new webpack.optimize.UglifyJsPlugin({
        minimize: true,
        sourceMap: true,
        compress: {
          warnings: false,
        },
        mangle: {
          except: ['$super', '$', 'exports', 'require'],
        },
      }),
      new webpack.optimize.OccurenceOrderPlugin(),
    ],
  });
}

if (TARGET === 'test') {
  console.log('Running test/ci mode');
  module.exports = merge(webpackConfig, {
    module: {
      preLoaders: [
        { test: /\.js(x)?$/, loader: 'eslint-loader', exclude: /node_modules|public\/javascripts/ }
      ],
    },
  });
}

if (Object.keys(module.exports).length === 0) {
  console.log('Running in default mode');
  module.exports = webpackConfig;
}
