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
// This file has been automatically migrated to valid ESM format by Storybook.
import path from 'path';
import { fileURLToPath } from 'url';

import type { StorybookConfig } from '@storybook/react-webpack5';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const webInterfaceRoot = path.resolve(__dirname, '../../..');

const config: StorybookConfig = {
  stories: ['../stories/**/*.stories.@(js|jsx|mjs|ts|tsx)'],
  addons: ['@storybook/addon-webpack5-compiler-swc', '@storybook/addon-docs', 'storybook-dark-mode'],
  framework: {
    name: '@storybook/react-webpack5',
    options: {},
  },
  webpackFinal: (sbConfig) => ({
    ...sbConfig,
    resolve: {
      ...sbConfig.resolve,
      modules: [
        path.resolve(webInterfaceRoot, 'src'),
        path.resolve(webInterfaceRoot, 'node_modules'),
        path.resolve(webInterfaceRoot, 'public'),
        ...(sbConfig.resolve?.modules ?? []),
      ],
      alias: {
        ...(sbConfig.resolve?.alias ?? {}),
        '@graylog/server-api': path.resolve(webInterfaceRoot, 'target/api'),
      },
    },
  }),
};

export default config;
