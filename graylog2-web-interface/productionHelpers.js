/* eslint-disable no-console */
const fs = require('fs');

const path = require('path');
const express = require('express');

const VENDORMODULE = 'vendor-module.json';
const BUILDMODULE = 'module.json';

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

function bootstrapExpress(buildDir, config, pluginMounts, indexHtml = '<html><body></body></html>', PORT) {
  const app = express();
  app.get('/', (req, res) => res.send(indexHtml));
  app.get('/assets/config.js', (req, res) => res.send(config));
  app.use('/assets', express.static(buildDir));

  // history fallback
  app.use((req, res, next) => {
    if ((req.method === 'GET' || req.method === 'HEAD') && req.accepts('html')) {
      res.send(indexHtml);
      next();
    } else {
      next();
    }
  });

  Object.entries(pluginMounts)
    .forEach(([name, pluginPath]) => app.use(`/assets/${name}`, express.static(pluginPath)));

  const server = app.listen(PORT);
  const { port } = server.address();

  return { url: `http://localhost:${port}`, server };
}

const buildDir = process.argv[2] || 'target/web/build';

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

module.exports = {
  bootstrapExpress,
  collectAssets,
  collectMounts,
  config,
  generateIndexHtml,
};
