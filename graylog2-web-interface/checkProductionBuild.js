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
/* eslint-disable no-console */
const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');
const express = require('express');
const cors = require('cors');

const VENDORMODULE = 'vendor-module.json';
const BUILDMODULE = 'module.json';

const HEADLESS_SHELL = 'test/bin/headless_shell';

const isExecutable = (filename) => {
  try {
    // eslint-disable-next-line no-bitwise
    fs.accessSync(filename, fs.constants.R_OK | fs.constants.X_OK);
  } catch (e) {
    return false;
  }
  return true;
};

const useHeadlessShell = (filename = HEADLESS_SHELL) => {
  const isLinux = process.platform === 'linux';
  if (!isLinux) {
    return false;
  }
  if (!fs.existsSync(filename)) {
    return false;
  }
  if (!isExecutable(filename)) {
    try {
      fs.chmodSync(filename, 0o755);
    } catch (e) {
      return false;
    }
  }
  return true;
};

function fatal(throwable) {
  console.error(throwable);
  process.exit(128);
}

function generateIndexHtml(assets) {
  return `
    <html>
      <head>
      </head>
      <body>
        ${assets.map((asset) => `<script src="${asset}"></script>`).join('\n')}
      </body>
    <html>
  `;
}
function bootstrapExpress(buildDir, config, pluginMounts, indexHtml = '<html><body></body></html>') {
  const app = express();
  app.get('/', (req, res) => res.send(indexHtml));
  app.get('/assets/config.js', (req, res) => res.send(config));
  app.use('/assets', express.static(buildDir));
  Object.entries(pluginMounts)
    .forEach(([name, pluginPath]) => app.use(`/assets/${name}`, express.static(pluginPath)));
  const server = app.listen();
  const { port } = server.address();

  return { url: `http://localhost:${port}`, server };
}

function bootstrapApi(prefix = '/api/') {
  const api = express();
  api.use(cors());

  const rootHandler = (req, res) => res.json({ cluster_id: 'deadbeef', node_id: 'deadbeef', version: '3.0.0', tagline: 'Manage your logs in the dark and have lasers going and make it look like you\'re from space!' });
  api.get(prefix, rootHandler);

  const sessionHandler = (req, res) => res.json({ session_id: null, username: null, is_valid: false });
  api.get(`${prefix}system/sessions`, sessionHandler);
  const authtypeHandler = (req, res) => res.json({ backend: 'okta' });
  api.get(`${prefix}system/authentication/services/backends/active-backend/type`, authtypeHandler);

  const server = api.listen();
  const { port } = server.address();

  return { url: `http://localhost:${port}${prefix}`, server };
}

process.setMaxListeners(0);

async function loadPage(url, handleError, handleConsole) {
  try {
    const options = {
      headless: true,
      args: ['--no-sandbox', '--disable-setuid-sandbox'],
    };
    if (useHeadlessShell()) {
      options.executablePath = HEADLESS_SHELL;
    }
    const browser = await puppeteer.launch(options);
    const page = await browser.newPage();
    page.on('console', handleConsole);
    page.on('pageerror', handleError);

    await page.goto(url, { waitUntil: 'networkidle0', timeout: 30000 });

    return { page, browser };
  } catch (e) {
    return fatal(e);
  }
}

const buildDir = process.argv[2] || 'target/web/build';
const plugins = process.argv.slice(3);

const config = (url) => `
window.appConfig = {
  gl2ServerUrl: '${url}',
  gl2AppPathPrefix: '/',
  rootTimeZone: 'UTC',
};
`;

function collectMounts(pluginModuleNames) {
  return pluginModuleNames.map((pluginModuleName) => {
    const pluginModule = JSON.parse(fs.readFileSync(pluginModuleName));
    const name = Object.keys(pluginModule.files.chunks)[0];
    return { [name]: path.dirname(pluginModuleName) };
  }).reduce((prev, cur) => ({ ...prev, ...cur }), {});
}

function collectAssets(pluginModules) {
  const vendorModule = JSON.parse(fs.readFileSync(`${buildDir}/${VENDORMODULE}`));
  const buildModule = JSON.parse(fs.readFileSync(`${buildDir}/${BUILDMODULE}`));
  const pluginAssets = pluginModules.flatMap((pluginModule) => pluginModule.files.js);

  return [
    'config.js',
    ...vendorModule.files.js,
    ...buildModule.files.js,
    ...pluginAssets,
  ].map((asset) => `/assets/${asset}`);
}

const pluginModules = plugins.map((plugin) => JSON.parse(fs.readFileSync(plugin)));
const assets = collectAssets(pluginModules);
const pluginMounts = collectMounts(plugins);

const api = bootstrapApi();
const { url, server } = bootstrapExpress(buildDir, config(api.url), pluginMounts, generateIndexHtml(assets));

const pageErrors = [];
const consoleLogs = [];

const trackEvent = (evt, arr) => {
  console.error(evt);
  arr.push(evt);
};

const pagePromise = loadPage(url, (msg) => trackEvent(msg, pageErrors), (msg) => trackEvent(msg, consoleLogs));
pagePromise
  .catch((err) => {
    console.error('Error: ', err.toString());
    process.exitCode = 1;
  })
  .finally(() => {
    const isSuccess = pageErrors.length === 0 && consoleLogs.length === 0;
    if (pageErrors.length > 0) {
      console.log('Errors:');
      console.log(pageErrors);
    }

    if (consoleLogs.length > 0) {
      console.log('Console Logs:');
      console.log(consoleLogs);
    }

    console.log(`\n${isSuccess ? 'Success' : 'Failure'}: Encountered ${pageErrors.length} errors and ${consoleLogs.length} messages on the console during loading.`);
    process.exitCode = isSuccess ? 0 : 1;
  })
  .finally(async () => {
    const { browser } = await pagePromise;
    await browser.close();
    server.close();
    api.server.close();
  });
