import path from 'path';

import mainAppWebpackConfig from '../../webpack.config.js';

export default async ({ config }) => {
  // Merge the main app's config with Storybook's config
  const mergedConfig = {
    ...config,
    ...mainAppWebpackConfig,
    resolve: {
      ...(config.resolve || {}), // Keep existing Storybook resolve settings)
      ...(mainAppWebpackConfig.resolve || {}), // Merge in main app resolve settings
      alias: {
        ...(config.resolve ? config.resolve.alias : {}), // Existing Storybook aliases
        ...(mainAppWebpackConfig.resolve && mainAppWebpackConfig.resolve.alias
          ? mainAppWebpackConfig.resolve.alias
          : {}), // Main app aliases
        '@graylog': path.resolve(__dirname, '../../src/'),
      },
    },
  };

  return mergedConfig;
};
