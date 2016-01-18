const webpack = require('webpack');
const WebpackCleanupPlugin = require('webpack-cleanup-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const merge = require('webpack-merge');
const path = require('path');
const VENDOR_MANIFEST = require('graylog-web-manifests/vendor-manifest.json');
const SHARED_MANIFEST = require('graylog-web-manifests/shared-manifest.json');
const TARGET = process.env.npm_lifecycle_event;

const PluginWebpackConfig = function(fqcn, options, additionalConfig) {
  const moduleJsonTemplate = path.resolve(module.parent.filename, '../templates/module.json.template');
  const config = {
    entry: {
    },
    output: {
      path: options.build_path,
      filename: '[name].[hash].js',
      publicPath: '',
    },
    module: {
      loaders: [
        { test: /\.(woff(2)?|svg|eot|ttf|gif|jpg)(\?.+)?$/, loader: 'file-loader' },
        { test: /\.png$/, loader: 'url-loader' },
        { test: /\.less$/, loaders: ['style', 'css', 'less'] },
        { test: /\.css$/, loaders: ['style', 'css'] },
        { test: /\.json$/, loader: 'json-loader' },
        { test: /\.js(x)?$/, loader: 'babel-loader', exclude: /node_modules|\.node_cache/ }
      ],
    },
    plugins: [
      new HtmlWebpackPlugin({filename: 'module.json', template: moduleJsonTemplate}),
      new webpack.DllReferencePlugin({ manifest: VENDOR_MANIFEST, context: options.root_path }),
      new webpack.DllReferencePlugin({ manifest: SHARED_MANIFEST, context: options.root_path }),
    ],
    resolve: {
      extensions: ['', '.js', '.json', '.jsx'],
      modulesDirectories: ['src/web', 'node_modules'],
    },
  };
  
  config.entry['plugin.' + fqcn] = options.entry_path;

  if (TARGET === 'build') {
    config.plugins.push(new WebpackCleanupPlugin({}));
  }

  if (additionalConfig) {
    return merge.smart(config, additionalConfig);
  }
  
  
  return config;
}

module.exports = PluginWebpackConfig;
