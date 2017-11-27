const express = require('express');
const webpack = require('webpack');
const compress = require('compression');
const history = require('connect-history-api-fallback');
const http = require('http');
const yargs = require('yargs');
const webpackDevMiddleware = require('webpack-dev-middleware');
const webpackHotMiddleware = require('webpack-hot-middleware');
const webpackConfig = require('./webpack.bundled');

const DEFAULT_PORT = 8081;

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

const server = http.createServer(app);

const argv = yargs.argv;

server
  .listen(argv.port || DEFAULT_PORT, () => {
    console.log(`Graylog web interface listening on port ${server.address().port}!\n`);
  })
  .on('error', (error) => {
    if (error.code === 'EADDRINUSE') {
      console.error(`Port ${argv.port || DEFAULT_PORT} already in use, will use a random one instead...`);
      server.listen(0);
    } else {
      throw error;
    }
  });
