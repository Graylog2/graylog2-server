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
import * as fs from 'fs';
import * as path from 'path';

import { globSync } from 'glob';

const WEB_MODULES = path.resolve(__dirname, './web-modules.json');

const configsFromWebModule = (webModulesFile) => {
  const webModules = JSON.parse(fs.readFileSync(webModulesFile).toString());

  return webModules.modules
    .map(({ path: p }) => p)
    .filter((_path) => _path.includes('graylog-plugin'))
    .map((_path) => `${_path}/webpack.config.ts`);
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

const pluginConfigFiles =
  process.env.disable_plugins === 'true'
    ? []
    : fs.existsSync(WEB_MODULES)
      ? configsFromWebModule(WEB_MODULES)
      : configsFromGlob();

if (pluginConfigFiles.some((config) => config.includes('graylog-plugin-cloud/server-plugin'))) {
  // @ts-ignore
  process.env.IS_CLOUD = true;
}

process.env.web_src_path = path.resolve(__dirname);

// eslint-disable-next-line @typescript-eslint/no-require-imports
const webpackConfig = require(path.resolve(__dirname, './webpack.config.ts'));
// eslint-disable-next-line global-require,@typescript-eslint/no-require-imports
const pluginConfigs = pluginConfigFiles.map((file) => require(file));

const allConfigs = [webpackConfig, ...pluginConfigs];

export default allConfigs;
