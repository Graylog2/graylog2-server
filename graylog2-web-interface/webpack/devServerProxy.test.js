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
/**
 * @jest-environment node
 */
import http from 'http';

import express from 'express';
import { createProxyMiddleware } from 'http-proxy-middleware';

import { createDevServerProxy, URL_HEADER } from './devServerProxy';

const listen = (server) =>
  new Promise((resolve) => {
    server.listen(0, '127.0.0.1', () => resolve(server.address().port));
  });

const get = (url, headers = {}) =>
  new Promise((resolve, reject) => {
    http.get(url, { headers }, (res) => res.resume().on('end', resolve)).on('error', reject);
  });

const close = (server) =>
  new Promise((resolve) => {
    server.close(resolve);
  });

describe('devServerProxy', () => {
  let backend;
  let proxy;
  let proxyUrl;
  let backendHeaders;

  beforeEach(async () => {
    backend = http.createServer((req, res) => {
      backendHeaders = req.headers;
      res.end();
    });
    const backendPort = await listen(backend);

    // Builds the middlewares the same way webpack-dev-server does, so option renames in `http-proxy-middleware` fail here
    const app = express();
    createDevServerProxy(`http://127.0.0.1:${backendPort}`).forEach((proxyConfig) =>
      app.use(createProxyMiddleware({ ...proxyConfig, pathFilter: proxyConfig.context })),
    );
    proxy = http.createServer(app);
    proxyUrl = `http://127.0.0.1:${await listen(proxy)}`;
  });

  afterEach(async () => {
    proxy.closeAllConnections();
    backend.closeAllConnections();
    await Promise.all([close(proxy), close(backend)]);
  });

  it('passes the URL used to reach the dev server to the backend', async () => {
    await get(`${proxyUrl}/api/system`);

    expect(backendHeaders[URL_HEADER.toLowerCase()]).toBe(proxyUrl);
  });

  it('keeps an existing URL header', async () => {
    await get(`${proxyUrl}/api/system`, { [URL_HEADER]: 'https://graylog.example.org/' });

    expect(backendHeaders[URL_HEADER.toLowerCase()]).toBe('https://graylog.example.org/');
  });
});
