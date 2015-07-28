// webpack.config.js
module.exports = {
    entry: './src/mount.jsx',
    output: {
        filename: '../app/assets/javascripts/build/app.js'
    },
    module: {
        preLoaders: [
            //{test: /\.js$/, loader: "eslint-loader", exclude: /node_modules/}
        ],
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