import path from 'path';
import mainAppWebpackConfig from '../../webpack.config.js';

export default async ({ config }) => {
  // Use the main app's config as a base
  // eslint-disable-next-line no-param-reassign
  config = mainAppWebpackConfig;

  // You can still extend or override specific rules for Storybook if needed
  // Example:
  // config.module.rules.push({
  //   test: /\.css$/,
  //   use: ['style-loader', 'css-loader'],
  //   include: path.resolve(__dirname, '../'),
  // });
  config.resolve.alias = {
    '@graylog': path.resolve(__dirname, '../../src/'),
  };

  return config;
};
