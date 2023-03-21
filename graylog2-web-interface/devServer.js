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
const http = require('http');

const express = require('express');
const webpack = require('webpack');
const compress = require('compression');
const history = require('connect-history-api-fallback');
const proxy = require('express-http-proxy');
const yargs = require('yargs');
const webpackDevMiddleware = require('webpack-dev-middleware');
const webpackHotMiddleware = require('webpack-hot-middleware');

const webpackConfigs = require('./webpack.bundled');

const DEFAULT_HOST = '127.0.0.1';
const DEFAULT_PORT = 8080;
const DEFAULT_API_URL = 'http://127.0.0.1:9000';

const app = express();
// Use two compilers to avoid re-compiling the vendor config on every change.
// We assume dependencies won't change while the server is running.
const pluginCompilers = webpackConfigs.map((config) => webpack(config));

const { argv } = yargs;
const host = argv.host || DEFAULT_HOST;
const port = argv.port || DEFAULT_PORT;
const apiUrl = argv.apiUrl || process.env.GRAYLOG_API_URL || DEFAULT_API_URL;

// Proxy all "/api" requests to the server backend API.
// eslint-disable-next-line no-console
console.log(`Graylog web interface forwarding /api requests to ${apiUrl}`);

app.use('/api', proxy(apiUrl, {
  proxyReqPathResolver(req) {
    // The proxy middleware removes the prefix from the path but we need it
    // to make sure we hit the "/api" resources on the server.
    return `/api${req.url}`;
  },
  parseReqBody: false,
}));

app.use('/config.js', proxy(apiUrl, {
  // proxy all requests to /config.js to the server backend API
  proxyReqPathResolver: () => '/config.js',
  parseReqBody: false,
}));

app.use(compress()); // Enables compression middleware
app.use(history()); // Enables HTML5 History API middleware

pluginCompilers.forEach((compiler, idx) => {
  app.use(webpackDevMiddleware(compiler, {
    publicPath: '/',
  }));

  if (idx > 0) {
    app.use(webpackHotMiddleware(compiler));
  }
});

const server = http.createServer(app);

server
  .listen(port, host, () => {
    // eslint-disable-next-line no-console
    console.log(`Graylog web interface listening on http://${server.address().address}:${server.address().port}!\n`);
  })
  .on('error', (error) => {
    if (error.code === 'EADDRINUSE') {
      // eslint-disable-next-line no-console
      console.error(`Address http://${host}:${port} already in use, will use a random one instead...`);
      server.listen(0, host);
    } else {
      throw error;
    }
  });
