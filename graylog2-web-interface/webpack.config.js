const fs = require('fs');
const webpack = require('webpack');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const merge = require('webpack-merge');

const ROOT_PATH = path.resolve(__dirname);
const APP_PATH = path.resolve(ROOT_PATH, 'src');
const BUILD_PATH = path.resolve(ROOT_PATH, 'build');
const MANIFESTS_PATH = path.resolve(ROOT_PATH, 'manifests');
const VENDOR_MANIFEST_PATH = path.resolve(MANIFESTS_PATH, 'vendor-manifest.json');
const TARGET = process.env.npm_lifecycle_event;
process.env.BABEL_ENV = TARGET;

const BABELRC = path.resolve(ROOT_PATH, '.babelrc');
const BABELOPTIONS = {
  cacheDirectory: 'cache',
  'extends': BABELRC,
};

const BABELLOADER = { loader: 'babel-loader', options: BABELOPTIONS };

const BOOTSTRAPVARS = require(path.resolve(ROOT_PATH, 'public', 'stylesheets', 'bootstrap-config.json')).vars;

const webpackConfig = {
  name: 'app',
  dependencies: ['vendor'],
  entry: {
    app: APP_PATH,
    polyfill: ['babel-polyfill'],
  },
  output: {
    path: BUILD_PATH,
    filename: '[name].[hash].js',
  },
  module: {
    rules: [
      { test: /pages\/.+\.jsx$/, use: 'react-proxy-loader', exclude: /node_modules|\.node_cache|ServerUnavailablePage/ },
      { test: /\.js(x)?$/, use: BABELLOADER, exclude: /node_modules|\.node_cache/ },
      { test: /\.ts$/, use: [BABELLOADER, { loader: 'ts-loader' }], exclude: /node_modules|\.node_cache/ },
      { test: /\.(woff(2)?|svg|eot|ttf|gif|jpg)(\?.+)?$/, use: 'file-loader' },
      { test: /\.png$/, use: 'url-loader' },
      { test: /bootstrap\.less$/, use: [
        'style-loader',
        'css-loader',
        {
          loader: 'less-loader',
          options: {
            modifyVars: BOOTSTRAPVARS,
          },
        },
      ] },
      { test: /\.less$/, use: ['style-loader', 'css-loader', 'less-loader'], exclude: /bootstrap\.less$/ },
      { test: /\.css$/, use: ['style-loader', 'css-loader'] },
    ],
  },
  resolve: {
    // you can now require('file') instead of require('file.coffee')
    extensions: ['.js', '.json', '.jsx', '.ts'],
    modules: [APP_PATH, 'node_modules', path.resolve(ROOT_PATH, 'public')],
  },
  resolveLoader: { modules: [path.join(ROOT_PATH, 'node_modules')], moduleExtensions: ['-loader'] },
  devtool: 'source-map',
  plugins: [
    new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST_PATH, context: ROOT_PATH }),
    new HtmlWebpackPlugin({
      title: 'Graylog',
      favicon: path.resolve(ROOT_PATH, 'public/images/favicon.png'),
      filename: 'index.html',
      inject: false,
      template: path.resolve(ROOT_PATH, 'templates/index.html.template'),
      vendorModule: () => JSON.parse(fs.readFileSync(path.resolve(ROOT_PATH, 'build/vendor-module.json'), 'utf8')),
      chunksSortMode: (c1, c2) => {
        // Render the polyfill chunk first
        if (c1.names[0] === 'polyfill') {
          return -1;
        }
        if (c2.names[0] === 'polyfill') {
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
  ],
};

if (TARGET === 'start') {
  console.error('Running in development mode');
  module.exports = merge(webpackConfig, {
    entry: {
      reacthot: 'react-hot-loader/patch',
    },
    devtool: 'eval',
    devServer: {
      historyApiFallback: true,
      hot: true,
      inline: true,
      lazy: false,
      watchOptions: {
        ignored: /node_modules/,
      },
    },
    output: {
      path: BUILD_PATH,
      filename: '[name].js',
      publicPath: '/',
      hotUpdateChunkFilename: '[id].hot-update.js',
      hotUpdateMainFilename: 'hot-update.json',
    },
    plugins: [
      new webpack.NamedModulesPlugin(),
      new webpack.DefinePlugin({DEVELOPMENT: true}),
    ],
  });
}

if (TARGET === 'start-nohmr') {
  console.error('Running in development (no HMR) mode');
  module.exports = merge(webpackConfig, {
    devtool: 'eval',
    devServer: {
      historyApiFallback: true,
      hot: false,
      inline: true,
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
  console.error('Running in production mode');
  process.env.NODE_ENV = 'production';
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
      new webpack.LoaderOptionsPlugin({
        minimize: true
      }),
    ],
  });
}

if (TARGET === 'test') {
  console.error('Running test/ci mode');
  module.exports = merge(webpackConfig, {
    module: {
      rules: [
        { test: /\.js(x)?$/, enforce: 'pre', loader: 'eslint-loader', exclude: /node_modules|public\/javascripts/ }
      ],
    },
  });
}

if (Object.keys(module.exports).length === 0) {
  module.exports = webpackConfig;
}
