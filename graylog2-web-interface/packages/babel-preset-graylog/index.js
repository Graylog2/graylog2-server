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
/* eslint-disable global-require */
module.exports = (api) => {
  const isEnvTest = api.env('test');

  return isEnvTest
    ? {
      presets: [
        require('@babel/preset-env'),
        require('@babel/preset-react'),
        require('@babel/preset-typescript')],
      plugins: [
        require('@babel/plugin-syntax-dynamic-import'),
        require('babel-plugin-styled-components'),
        require('babel-plugin-dynamic-import-node'),
        require('@babel/plugin-transform-runtime'),
      ],
    }
    : {
      presets: [
        [require('@babel/preset-env'), {
          modules: false,
          useBuiltIns: 'entry',
          corejs: '3.9',
          shippedProposals: true,
        }],
        require('@babel/preset-react'),
        require('@babel/preset-typescript'),
      ],
      plugins: [
        require('@babel/plugin-syntax-dynamic-import'),
        require('babel-plugin-styled-components'),
      ],
    };
};
