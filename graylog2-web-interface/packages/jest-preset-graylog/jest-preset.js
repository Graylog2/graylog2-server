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

// eslint-disable-next-line @typescript-eslint/no-require-imports,no-undef
const { applyTimeoutMultiplier } = require('./lib/timeouts');

// eslint-disable-next-line no-undef
module.exports = {
  rootDir: '../../',
  collectCoverageFrom: ['src/**/*.{js,jsx,ts,tsx}'],
  setupFiles: [
    /* eslint-disable no-undef */
    require.resolve('./lib/setup-files/mock-FetchProvider.js'),
    require.resolve('./lib/setup-files/mock-Version.js'),
    require.resolve('./lib/setup-files/mock-IntersectionObserver.js'),
    require.resolve('./lib/setup-files/mock-ResizeObserver.js'),
    require.resolve('./lib/setup-files/mock-matchMedia.js'),
    require.resolve('./lib/setup-files/mock-moment-timezone.js'),
    require.resolve('./lib/setup-files/console-warnings-fail-tests.js'),
    require.resolve('./lib/setup-files/mock-crypto-getrandomvalues.js'),
    require.resolve('./lib/setup-files/mock-styled-components.js'),
    require.resolve('./lib/setup-files/mock-AppConfig.js'),
    /* eslint-enable no-undef */
    'jest-canvas-mock',
  ],
  moduleDirectories: ['src', 'test', 'node_modules'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx'],
  moduleNameMapper: {
    '^file-loader(\\?esModule=false)?!(.+)$': '$2',
    // eslint-disable-next-line no-undef
    '(\\.lazy|leaflet)\\.css$': require.resolve('./lib/mocking/useable-css-proxy.js'),
    '\\.(css|less)$': 'identity-obj-proxy',
    '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
      // eslint-disable-next-line no-undef
      require.resolve('./lib/mocking/fileMock.js'),
    '^@graylog/server-api(.*)$': '<rootDir>/target/api$1',
  },
  testEnvironment: 'jsdom',
  testPathIgnorePatterns: ['.fixtures.[jt]s$', '^<rootDir>/target/'],
  testTimeout: applyTimeoutMultiplier(5000),
  transform: {
    // Mirror the previous babel-preset-graylog behaviour:
    // - target es5 downlevels `let`/`const` to `var`, which is required because several test
    //   files reference variables from hoisted `jest.mock` factories (TDZ-safe only with `var`).
    // - the classic React runtime extracts `key` from spread props like `React.createElement` did.
    // - `useDefineForClassFields: true` keeps babel's define semantics for class fields.
    '^.+\\.[tj]sx?$': ['@swc/jest', {
      jsc: {
        parser: { syntax: 'typescript', tsx: true, decorators: true },
        transform: {
          react: { runtime: 'classic' },
          useDefineForClassFields: true,
        },
        target: 'es5',
        loose: false,
      },
      module: { type: 'commonjs' },
    }],
  },
  transformIgnorePatterns: [
    'node_modules/(?!(@react-hook|uuid|@?react-leaflet|jest-preset-graylog|graylog-web-plugin|styled-components|p-debounce|marked|d3-(interpolate|color))/)',
  ],
};
