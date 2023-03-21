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
const fs = require('fs');
const path = require('path');

const { globSync } = require('glob');

const TARGET = process.env.npm_lifecycle_event;
const WEB_MODULES = path.resolve(__dirname, './web-modules.json');

const configsFromWebModule = (webModulesFile) => {
  const webModules = JSON.parse(fs.readFileSync(webModulesFile));

  return webModules.modules
    .map(({ path: p }) => p)
    .filter((_path) => _path.includes('graylog-plugin'))
    .map((_path) => `${_path}/webpack.config.js`);
};

const configsFromGlob = () => {
  const pluginConfigPattern = 'graylog-plugin-*/**/webpack.config.js';
  const globCwd = '../..';
  const globOptions = {
    ignore: '**/node_modules/**',
    cwd: globCwd,
    nodir: true,
  };

  function isNotDependency(pluginConfig) {
    // Avoid including webpack configs of dependencies and built files.
    return !pluginConfig.includes('/target/') && !pluginConfig.includes('/node_modules/');
  }

  return globSync(pluginConfigPattern, globOptions)
    .map((config) => `${globCwd}/${config}`)
    .filter(isNotDependency);
};

// eslint-disable-next-line no-nested-ternary
const pluginConfigFiles = process.env.disable_plugins === 'true'
  ? []
  : fs.existsSync(WEB_MODULES)
    ? configsFromWebModule(WEB_MODULES)
    : configsFromGlob();

if (pluginConfigFiles.some((config) => config.includes('graylog-plugin-cloud/server-plugin'))) {
  process.env.IS_CLOUD = true;
}

process.env.web_src_path = path.resolve(__dirname);

// eslint-disable-next-line import/no-dynamic-require
const webpackConfig = require(path.resolve(__dirname, './webpack.config.js'));
// eslint-disable-next-line import/no-dynamic-require,global-require
const pluginConfigs = pluginConfigFiles.map((file) => require(file));

const allConfigs = [webpackConfig, ...pluginConfigs];

// We need to inject webpack-hot-middleware to all entries, ensuring the app is able to reload on changes.
if (TARGET === 'start') {
  const webpackHotMiddlewareEntry = 'webpack-hot-middleware/client?reload=true';

  allConfigs.forEach((finalConfig) => {
    const hmrEntries = {};

    Object.keys(finalConfig.entry).forEach((entryKey) => {
      const entryValue = finalConfig.entry[entryKey];
      hmrEntries[entryKey] = [webpackHotMiddlewareEntry].concat(Array.isArray(entryValue) ? entryValue : [entryValue]);
    });

    // eslint-disable-next-line no-param-reassign
    finalConfig.entry = hmrEntries;
  });
}

module.exports = allConfigs;
