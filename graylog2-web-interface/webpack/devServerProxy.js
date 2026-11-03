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
const URL_HEADER = 'X-Graylog-Server-URL';

const setUrlHeader = (proxyReq, url) => {
  if (!proxyReq.getHeader(URL_HEADER)?.toString().trim()) {
    proxyReq.setHeader(URL_HEADER, url);
  }
};

const createDevServerProxy = (apiUrl) => [
  {
    context: ['/api', '/config.js', '/sso', '/.well-known', '/v1/opamp'],
    target: apiUrl,
    ws: true,
    // Skip proxying for /api-browser - it's a frontend route, not an API endpoint
    bypass: (req) => (req.path.startsWith('/api-browser') ? req.path : null),
    on: {
      proxyReq: (proxyReq, req) => setUrlHeader(proxyReq, `${req.protocol}://${req.get('host')}`),
      proxyReqWs: (proxyReq, req) => setUrlHeader(proxyReq, req.headers.origin || `http://${req.headers.host}`),
    },
  },
];

module.exports = { createDevServerProxy, URL_HEADER };
