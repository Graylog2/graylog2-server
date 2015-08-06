// webpack.shared.config.js
var Clean = require('clean-webpack-plugin');

var webpackConfig = {
    entry: ['./src/mount.jsx'],
    output: {
        path: '../public/javascripts/build/',
        filename: 'app.js'
    },
    module: {
        preLoaders: [
            //{ test: /\.js(x)?$/, loader: "eslint-loader", exclude: /node_modules/ }
        ],
        loaders: [
            { test: /\.json$/, loader: 'json-loader' },
            { test: /\.js(x)?$/, loaders: ['react-hot', 'babel-loader?stage=0'], exclude: /node_modules/ },
            { test: /\.ts$/, loader: 'awesome-typescript-loader?emitRequireType=false&library=es6', exclude: /node_modules/ }
        ]
    },
    resolve: {
        // you can now require('file') instead of require('file.coffee')
        extensions: ['', '.js', '.json', '.jsx', '.ts']
    },
    externals: {
        "jquery" : "jQuery"
    },
    eslint: {
        configFile: '.eslintrc'
    }
};

webpackConfig.plugins = [
    new Clean([webpackConfig.output.path])
];

module.exports = webpackConfig;
