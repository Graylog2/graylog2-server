/* eslint-disable no-console */
const fs = require('fs');

const productionHelpers = require('./productionHelpers');

process.setMaxListeners(0);

const buildDir = process.argv[2] || 'target/web/build';
const apiUrl = process.argv[3] || 'http://localhost:9000/api';
const plugins = process.argv.slice(4);

const pluginModules = plugins.map((plugin) => JSON.parse(fs.readFileSync(plugin)));
const assets = productionHelpers.collectAssets(pluginModules);
const pluginMounts = productionHelpers.collectMounts(plugins);

const { url } = productionHelpers.bootstrapExpress(
  buildDir,
  productionHelpers.config(apiUrl),
  pluginMounts,
  productionHelpers.generateIndexHtml(assets),
  '8080',
);

console.log('Running server at', url);
