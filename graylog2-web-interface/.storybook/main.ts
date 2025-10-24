import type { StorybookConfig } from '@storybook/react-webpack5';
import TsconfigPathsPlugin from 'tsconfig-paths-webpack-plugin';

const config: StorybookConfig = {
  stories: ['../src/**/*.mdx', '../src/**/*.stories.@(js|jsx|mjs|ts|tsx)'],
  addons: ['@storybook/addon-webpack5-compiler-swc', '@storybook/addon-docs', 'storybook-dark-mode'],
  framework: {
    name: '@storybook/react-webpack5',
    options: {},
  },
  // eslint-disable-next-line @typescript-eslint/no-shadow
  webpackFinal: async (config) => {
    if (config.resolve) {
      // eslint-disable-next-line no-param-reassign
      config.resolve.plugins = [
        ...(config.resolve.plugins || []),
        new TsconfigPathsPlugin({
          extensions: config.resolve.extensions,
        }),
      ];
    }

    return config;
  },
};
export default config;
