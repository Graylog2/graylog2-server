// webpack.shared.config.js
const Clean = require('clean-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

var webpackConfig = {
    entry: {
      app: './src/mount.jsx',
      config: './src/config.js',
    },
    output: {
        path: './public/build/',
        filename: '[name].js',
    },
    module: {
        preLoaders: [
            //{ test: /\.js(x)?$/, loader: 'eslint-loader', exclude: /node_modules/ }
        ],
        loaders: [
            { test: /\.json$/, loader: 'json-loader' },
            { test: /\.js(x)?$/, loaders: ['react-hot', 'babel-loader?stage=0'], exclude: /node_modules|\.node_cache/ },
            { test: /\.ts$/, loader: 'babel-loader!ts-loader', exclude: /node_modules|\.node_cache/ },
            { test: /\.jpg$/, loader: 'file-loader' },
            { test: /\.(woff(2)?|svg|eot|ttf)(\?.+)?$/, loader: 'file-loader' },
            { test: /\.png$/, loader: 'url-loader' },
            { test: /\.less$/, loader: 'style!css!less' },
            { test: /\.css$/, loader: 'style!css' },
        ]
    },
    resolve: {
        // you can now require('file') instead of require('file.coffee')
        extensions: ['', '.js', '.json', '.jsx', '.ts'],
        modulesDirectories: ['src', 'node_modules', 'public'],
    },
    eslint: {
        configFile: '.eslintrc',
    }
};

webpackConfig.plugins = [
    new Clean([webpackConfig.output.path]),
    new HtmlWebpackPlugin({title: 'Graylog', filename: '../index.html'}),
];

module.exports = webpackConfig;
