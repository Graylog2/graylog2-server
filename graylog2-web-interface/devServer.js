const express = require('express');
const webpack = require('webpack');
const compress = require('compression');
const history = require('connect-history-api-fallback');
const webpackDevMiddleware = require('webpack-dev-middleware');
const webpackHotMiddleware = require('webpack-hot-middleware');
const webpackConfig = require('./webpack.bundled');

const app = express();
const vendorConfig = webpackConfig[0];
const appConfig = webpackConfig[1];
// Use two compilers to avoid re-compiling the vendor config on every change.
// We assume dependencies won't change while the server is running.
const vendorCompiler = webpack(vendorConfig);
const appCompiler = webpack(appConfig);


app.use(compress()); // Enables compression middleware
app.use(history()); // Enables HTML5 History API middleware

app.use(webpackDevMiddleware(vendorCompiler, {
  publicPath: appConfig.output.publicPath,
  lazy: false,
  noInfo: true,
}));

app.use(webpackDevMiddleware(appCompiler, {
  publicPath: appConfig.output.publicPath,
  lazy: false,
  noInfo: true,
}));

app.use(webpackHotMiddleware(appCompiler));

app.listen(8080, () => {
  console.log('Graylog web interface listening on port 8080!\n');
});
