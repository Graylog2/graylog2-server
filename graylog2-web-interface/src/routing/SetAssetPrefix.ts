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
import URI from 'urijs';

import AppConfig from 'util/AppConfig';

declare let __webpack_public_path__: string;

const setAssetPrefix = (productionPrefix: string) => {
  // The webpack-dev-server serves the assets from "/"
  const assetPrefix = AppConfig.gl2DevMode() ? '/' : productionPrefix;

  // If app prefix was not set, we need to tell webpack to load chunks from root instead of the relative URL path
  if (AppConfig.gl2AppPathPrefix() !== undefined) {
    __webpack_public_path__ = URI.joinPaths(AppConfig.gl2AppPathPrefix(), assetPrefix).path() || assetPrefix;
  } else {
    throw new Error(
      `Unable to determine app prefix. Is your Graylog server (${AppConfig.gl2ServerUrl()}) running and reachable?`,
    );
  }
};

export default setAssetPrefix;
