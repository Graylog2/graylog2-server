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
module.exports = {
  rootDir: '../../',
  collectCoverageFrom: [
    'src/**/*.{js,jsx,ts,tsx}',
  ],
  setupFiles: [
    'jest-localstorage-mock',
    require.resolve('./mock-FetchProvider.js'),
    require.resolve('./mock-Version.js'),
    require.resolve('./console-warnings-fail-tests.js'),
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
    '^file-loader(\\?esModule=false)?!(.+)$': '$2',
    '^!style/useable!.*\\.(css|less)$': require.resolve('./css/useable-css-proxy.js'),
    '\\.(css|less)$': 'identity-obj-proxy',
    c3: require.resolve('./helpers/mocking/c3_mock.jsx'),
    '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$': require.resolve('./fileMock.js'),
  },
  testPathIgnorePatterns: [
    '.fixtures.[jt]s$',
  ],
};
