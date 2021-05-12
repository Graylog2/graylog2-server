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
const { applyTimeoutMultiplier } = require('./lib/timeouts');

module.exports = {
  rootDir: '../../',
  collectCoverageFrom: [
    'src/**/*.{js,jsx,ts,tsx}',
  ],
  setupFiles: [
    'jest-localstorage-mock',
    require.resolve('./lib/setup-files/mock-FetchProvider.js'),
    require.resolve('./lib/setup-files/mock-Version.js'),
    require.resolve('./lib/setup-files/mock-IntersectionObserver.js'),
    require.resolve('./lib/setup-files/console-warnings-fail-tests.js'),
    'jest-canvas-mock',
  ],
  setupFilesAfterEnv: [
    'jest-enzyme',
  ],
  moduleDirectories: [
    'src',
    'test',
    'node_modules',
  ],
  moduleFileExtensions: [
    'ts',
    'tsx',
    'js',
    'jsx',
  ],
  moduleNameMapper: {
    c3: require.resolve('./lib/mocking/c3_mock.js'),
    '^file-loader(\\?esModule=false)?!(.+)$': '$2',
    '(\\.lazy|leaflet)\\.css$': require.resolve('./lib/mocking/useable-css-proxy.js'),
    '\\.(css|less)$': 'identity-obj-proxy',
    '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$': require.resolve('./lib/mocking/fileMock.js'),
  },
  testPathIgnorePatterns: [
    '.fixtures.[jt]s$',
  ],
  testTimeout: applyTimeoutMultiplier(5000),
  reporters: ['default', 'jest-junit'],
};
