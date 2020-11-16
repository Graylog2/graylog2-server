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

module.exports = function loadBuildConfig(filename) {
  try {
    // eslint-disable-next-line global-require
    const buildConfig = require(filename);
    if (!buildConfig.web_src_path || !fs.lstatSync(buildConfig.web_src_path).isDirectory()) {
      /* eslint-disable no-console */
      console.error('Path to graylog web interface sources is not defined, does not exist or is not a directory: (', buildConfig.web_src_path, ').');
      console.error('Please configure it in a file named `build.config.js` before trying to build the plugin.');
      // TODO: add link to documentation
      console.error('For further information please check http://docs.graylog.org/PLACEHOLDER');
      process.exit(-1);
      /* eslint-enable no-console */
    }
    return buildConfig;
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error('It seems like there is no readable build.config.js file: ', e);
  }
  process.exit(-1);
  return undefined;
};
