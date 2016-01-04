const Clean = require('clean-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const merge = require('webpack-merge');

const PluginWebpackConfig = function(fqcn, path, additionalConfig) {
  const config = {
    entry: './src/mount.jsx',
    output: {
      path: path,
      filename: 'plugin.[hash].js',
      publicPath: '/plugin/' + fqcn + '/',
    },
    module: {
      loaders: [
        { test: /\.(woff(2)?|svg|eot|ttf|gif|jpg)(\?.+)?$/, loader: 'file-loader' },
        { test: /\.png$/, loader: 'url-loader' },
        { test: /\.less$/, loaders: ['style', 'css', 'less'] },
        { test: /\.css$/, loaders: ['style', 'css'] },
        { test: /\.js(x)?$/, loader: 'babel-loader', exclude: /node_modules|\.node_cache/ }
      ],
    },
    plugins: [
      new Clean([path]),
      new HtmlWebpackPlugin({filename: 'module.json', template: 'templates/module.json.template'}),
    ],
    resolve: {
      extensions: ['', '.js', '.json', '.jsx'],
      modulesDirectories: ['src'],
    },
  };
  
  if (additionalConfig) {
    return merge(config, additionalConfig);
  }
  
  return config;
}

module.exports = PluginWebpackConfig;
