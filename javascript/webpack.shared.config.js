// webpack.tests.config.js
module.exports = {
    module: {
        loaders: [
            { test: /\.json$/, loader: 'json-loader' },
            { test: /\.js(x)?$/, loader: 'babel-loader?stage=0', exclude: /node_modules/ },
            { test: /\.ts$/, loader: 'awesome-typescript-loader?emitRequireType=false&library=es6', exclude: /node_modules/ }
        ]
    },
    resolve: {
        // you can now require('file') instead of require('file.coffee')
        extensions: ['', '.js', '.json', '.jsx', '.ts']
    }
};