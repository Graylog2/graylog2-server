/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
const express = require('express');
const webpack = require('webpack');
const compress = require('compression');
const history = require('connect-history-api-fallback');
const http = require('http');
const yargs = require('yargs');
const webpackDevMiddleware = require('webpack-dev-middleware');
const webpackHotMiddleware = require('webpack-hot-middleware');
const webpackConfig = require('./webpack.bundled');

const DEFAULT_HOST = '127.0.0.1';
const DEFAULT_PORT = 8080;

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
const host = argv.host || DEFAULT_HOST;
const port = argv.port || DEFAULT_PORT;

server
  .listen(port, host, () => {
    console.log(`Graylog web interface listening on http://${server.address().address}:${server.address().port}!\n`);
  })
  .on('error', (error) => {
    if (error.code === 'EADDRINUSE') {
      console.error(`Address http://${host}:${port} already in use, will use a random one instead...`);
      server.listen(0, host);
    } else {
      throw error;
    }
  });
