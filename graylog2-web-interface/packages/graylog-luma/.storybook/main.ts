// This file has been automatically migrated to valid ESM format by Storybook.
/* eslint-disable no-param-reassign */
import path, { dirname } from 'path';
import { fileURLToPath } from 'url';

import TsconfigPaths from 'tsconfig-paths-webpack-plugin';
import type { StorybookConfig } from '@storybook/react-webpack5';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const config: StorybookConfig = {
  stories: ['../stories/**/*.mdx', '../stories/**/*.stories.@(js|jsx|mjs|ts|tsx)'],
  addons: ['@storybook/addon-webpack5-compiler-swc', '@storybook/addon-docs', 'storybook-dark-mode'],
  framework: {
    name: '@storybook/react-webpack5',
    options: {},
  },
  webpackFinal: async (sbConfig) => {
    if (sbConfig.resolve) {
      sbConfig.resolve.roots = [
        path.resolve(__dirname, '../node_modules'),
        path.resolve(__dirname, '../../../node_modules'),
      ];

      sbConfig.resolve.plugins = [
        ...(sbConfig.resolve.plugins || []),
        new TsconfigPaths({
          extensions: sbConfig.resolve.extensions,
        }),
      ];
    }

    // storybookConfig.resolve ??= {};
    //
    // storybookConfig.resolve.plugins = storybookConfig.resolve.plugins || [];
    // storybookConfig.resolve.plugins.push(
    //   new TsconfigPaths({
    //     extensions: storybookConfig.resolve.extensions,
    //     configFile: path.resolve(__dirname, '../../../tsconfig.json'),
    //   }),
    // );
    //

    return sbConfig;
  },
};

export default config;
