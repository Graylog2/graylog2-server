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
  presets: [['@babel/env', { modules: false }], '@babel/react', '@babel/flow', '@babel/typescript'],
  plugins: [
    '@babel/plugin-syntax-dynamic-import',
    '@babel/plugin-proposal-class-properties',
    'babel-plugin-styled-components',
  ],
  env: {
    test: {
      presets: ['@babel/env', '@babel/typescript'],
      plugins: [
        'babel-plugin-dynamic-import-node',
        '@babel/plugin-transform-runtime',
      ],
    },
  },
};
