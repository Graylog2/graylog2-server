// webpack.dev.config.js
var webpackSharedConfig = require("./webpack.shared.config");

var outputPath = '../public/javascripts/build/';
module.exports = {
    entry: './src/mount.jsx',
    output: {
        path: outputPath,
        filename: 'app.js'
    },
    module: {
        preLoaders: [
            //{ test: /\.js(x)?$/, loader: "eslint-loader", exclude: /node_modules/ }
        ],
        loaders: webpackSharedConfig.module.loaders
    },
    eslint: {
        configFile: '.eslintrc'
    },
    resolve: webpackSharedConfig.resolve,
    externals: {
        "jquery" : "jQuery"
    },
};