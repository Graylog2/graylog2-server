const fs = require('fs');
const puppeteer = require('puppeteer');
const express = require('express');

const VENDORMODULE = 'vendor-module.json';
const BUILDMODULE = 'module.json';

function bootstrapExpress(buildDir, config) {
  const app = express();
  app.get('/', (req, res) => res.send('<html><body></body></html>'));
  app.get('/assets/config.js', (req, res) => res.send(config));
  app.use('/assets', express.static(buildDir));
  const server = app.listen();
  const { port } = server.address();

  return { url: `http://localhost:${port}`, server };
}

process.setMaxListeners(0);

async function bootstrap(url, handleError, handleConsole) {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });
  const page = await browser.newPage();
  page.on('console', handleConsole);
  page.on('pageerror', handleError);

  await page.setBypassCSP(true);
  await page.goto(url);
  return { page, browser };
}

async function loadAssets(pagePromise, prefix, assets) {
  const { page } = await pagePromise;
  for (i in assets) {
    const assetUrl = `${prefix}/assets/${assets[i]}`;
    console.log(`Loading asset ${assetUrl}.`);
    await page.addScriptTag({ url: assetUrl });
  }
}

const buildDir = process.argv[2] || 'build';

const config = `
window.appConfig = {
  gl2ServerUrl: 'https://smd.torch.sh/api/',
  gl2AppPathPrefix: '/',
  rootTimeZone: 'UTC',
};
`;

const { url, server } = bootstrapExpress(buildDir, config);

const pageErrors = [];
const consoleLogs = [];

const pagePromise = bootstrap(url, (msg) => { pageErrors.push(msg); }, (msg) => { consoleLogs.push(msg); });

const vendorModule = JSON.parse(fs.readFileSync(`${buildDir}/${VENDORMODULE}`));
const buildModule = JSON.parse(fs.readFileSync(`${buildDir}/${BUILDMODULE}`));

const assets = [
  'config.js',
  ...vendorModule.files.js,
  buildModule.files.chunks.polyfill.entry,
  buildModule.files.chunks.builtins.entry,
  buildModule.files.chunks.app.entry,
];

loadAssets(pagePromise, url, assets)
  .then(() => {
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
  .then(async () => {
    const { browser } = await pagePromise;
    return browser.close();
  })
  .then(() => server.close());
