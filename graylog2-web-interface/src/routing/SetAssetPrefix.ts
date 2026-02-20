import URI from 'urijs';

import AppConfig from 'util/AppConfig';

const setAssetPrefix = (productionPrefix: string) => {
  // The webpack-dev-server serves the assets from "/"
  const assetPrefix = AppConfig.gl2DevMode() ? '/' : productionPrefix;

  // If app prefix was not set, we need to tell webpack to load chunks from root instead of the relative URL path

  if (AppConfig.gl2AppPathPrefix() !== undefined) {
    // @ts-expect-error
    __webpack_public_path__ = URI.joinPaths(AppConfig.gl2AppPathPrefix(), assetPrefix).path() || assetPrefix;
  } else {
    throw new Error(
      `Unable to determine app prefix. Is your Graylog server (${AppConfig.gl2ServerUrl()}) running and reachable?`,
    );
  }
};

export default setAssetPrefix;
