const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');
const express = require('express');

const VENDORMODULE = 'vendor-module.json';
const BUILDMODULE = 'module.json';

function generateIndexHtml(assets) {
  return `
    <html>
      <head>
      </head>
      <body>
        ${assets.map(asset => `<script src="${asset}"></script>`).join('\n')}
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

process.setMaxListeners(0);

async function loadPage(url, handleError, handleConsole) {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });
  const page = await browser.newPage();
  page.on('console', handleConsole);
  page.on('pageerror', handleError);

  await page.goto(url);
  return { page, browser };
}

const buildDir = process.argv[2] || 'build';
const plugins = process.argv.slice(3);

const config = `
window.appConfig = {
  gl2ServerUrl: 'http://localhost:9000/api/',
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
  const pluginAssets = pluginModules.map((pluginModule) => {
    const name = Object.keys(pluginModule.files.chunks)[0];
    const file = pluginModule.files.chunks[name].entry;
    return `${name}/${file}`;
  });

  return [
    'config.js',
    ...vendorModule.files.js,
    buildModule.files.chunks.polyfill.entry,
    buildModule.files.chunks.builtins.entry,
    ...pluginAssets,
    buildModule.files.chunks.app.entry,
  ].map(asset => `/assets/${asset}`);
}

const pluginModules = plugins.map(plugin => JSON.parse(fs.readFileSync(plugin)));
const assets = collectAssets(pluginModules);
const pluginMounts = collectMounts(plugins);

const { url, server } = bootstrapExpress(buildDir, config, pluginMounts, generateIndexHtml(assets));

const pageErrors = [];
const consoleLogs = [];

const pagePromise = loadPage(url, (msg) => { pageErrors.push(msg); }, (msg) => { consoleLogs.push(msg); });
pagePromise
  .catch(err => console.error('Error: ', err.toString()))
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
  });
