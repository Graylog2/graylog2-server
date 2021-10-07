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
/* This file contains configuration for React Styleguidist https://react-styleguidist.js.org/ */

const path = require('path');

const requireIt = require('react-styleguidist/lib/loaders/utils/requireIt').default;

const webpackConfig = require('./webpack.config.js');

const defaultComponentIgnore = [
  '**/__tests__/**',
  '**/*.test.{js,jsx,ts,tsx}',
  '**/*.spec.{js,jsx,ts,tsx}',
  '**/*.d.ts',
];

module.exports = {
  require: [
    'core-js/stable',
    'regenerator-runtime/runtime',
    'bootstrap/less/bootstrap.less',
    'toastr/toastr.less',
    'stylesheets/typeahead.less',
  ],
  // propsParser: require('react-docgen-typescript').parse,
  sections: [
    {
      name: 'Alert',
      components: 'src/components/graylog/Alert.tsx',
    },

  ],
  usageMode: 'collapse',
  styleguideComponents: {
    Wrapper: path.join(__dirname, 'docs/StyleGuideWrapper'),
  },
  styleguideDir: 'docs/styleguide',
  title: 'Graylog UI documentation',
  getExampleFilename(componentPath) {
    return componentPath.replace(/\.tsx?$/, '.example.tsx');
  },
  updateDocs(docs) {
    const loader = path.join(__dirname, 'webpack/examples-loader.js');
    const requirePath = docs.examples.require.replace(/^!!([^!]*)!/, `!!${loader}!`);
    docs.examples = requireIt(requirePath);

    return docs;
  },
  webpackConfig: {
    module: webpackConfig.module,
    resolve: webpackConfig.resolve,
    resolveLoader: webpackConfig.resolveLoader,
  },
};
