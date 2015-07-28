// webpack.config.js
var HtmlWebpackPlugin = require('html-webpack-plugin');
var webpackSharedConfig = require("./webpack.shared.config");

module.exports = {
    entry: './src/mount.jsx',
    output: {
        filename: '../app/assets/javascripts/build/app.[hash].js'
    },
    module: {
        preLoaders: [
            { test: /\.js(x)?$/, loader: "eslint-loader", exclude: /node_modules/ }
        ],
        loaders: webpackSharedConfig.module.loaders
    },
    plugins: [new HtmlWebpackPlugin({
        filename: '../app/views/build/scripts.scala.html',
        template: 'templates/scripts.scala.html.template'
    })],
    eslint: {
        configFile: '.eslintrc'
    },
    resolve: webpackSharedConfig.resolve,
    devtool: "#inline-source-map"
};