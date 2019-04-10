const fs = require('fs');
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
function bootstrapExpress(buildDir, config, indexHtml = '<html><body></body></html>') {
  const app = express();
  app.get('/', (req, res) => res.send(indexHtml));
  app.get('/assets/config.js', (req, res) => res.send(config));
  app.use('/assets', express.static(buildDir));
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

const config = `
window.appConfig = {
  gl2ServerUrl: 'https://smd.torch.sh/api/',
  gl2AppPathPrefix: '/',
  rootTimeZone: 'UTC',
};
`;

function collectAssets() {
  const vendorModule = JSON.parse(fs.readFileSync(`${buildDir}/${VENDORMODULE}`));
  const buildModule = JSON.parse(fs.readFileSync(`${buildDir}/${BUILDMODULE}`));

  return [
    'config.js',
    ...vendorModule.files.js,
    buildModule.files.chunks.polyfill.entry,
    buildModule.files.chunks.builtins.entry,
    buildModule.files.chunks.app.entry,
  ].map(asset => `/assets/${asset}`);
}

const assets = collectAssets();

const { url, server } = bootstrapExpress(buildDir, config, generateIndexHtml(assets));

const pageErrors = [];
const consoleLogs = [];

const pagePromise = loadPage(url, (msg) => { pageErrors.push(msg); }, (msg) => { consoleLogs.push(msg); });

pagePromise
  .catch(err => console.error('Error: ', err.toString()))
  .finally(() => {
    const isSuccess = pageErrors.length === 0 && consoleLogs.length === 0;
    console.log(`\nLoading completed, encountered ${pageErrors.length} errors and ${consoleLogs.length} messages on the console - ${isSuccess ? 'Success' : 'Failure'}!`);
    if (pageErrors.length > 0) {
      console.log('Errors:');
      console.log(pageErrors);
    }

    if (consoleLogs.length > 0) {
      console.log('Console Logs:');
      console.table(consoleLogs);
    }
  })
  .finally(async () => {
    const { browser } = await pagePromise;
    await browser.close();
    server.close();
  });
