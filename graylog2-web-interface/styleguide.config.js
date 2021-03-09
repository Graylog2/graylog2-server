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

const webpackConfig = require('./webpack.config.js');

module.exports = {
  require: [
    'bootstrap/less/bootstrap.less',
    'toastr/toastr.less',
    'stylesheets/typeahead.less',
    'injection/builtins.js',
  ],
  sections: [
    {
      name: 'Introduction',
      content: 'docs/introduction.md',
    },
    {
      name: 'Style Guide',
      content: 'docs/styleguide.md',
    },
    {
      name: 'Documentation',
      content: 'docs/documentation.md',
    },
    {
      name: 'Tests',
      content: 'docs/tests.md',
    },
    {
      name: 'Util Objects',
      content: 'docs/util-objects.md',
    },
    {
      name: 'Shared Components',
      sections: [
        {
          name: 'Bootstrap',
          components: 'src/components/bootstrap/[A-Z]*.{jsx,tsx}',
        },
        {
          name: 'Common',
          components: 'src/components/common/[A-Z]*.{jsx,tsx}',
        },
        {
          name: 'Themeable',
          components: 'src/components/graylog/[A-Z]*.{jsx,tsx}',
        },
        {
          name: 'Configuration Forms',
          components: 'src/components/configurationforms/[A-Z]*.{jsx,tsx}',
        },
      ],
    },
    {
      name: 'Theming Details',
      content: 'src/theme/docs/Details.md',
      sections: [
        {
          name: 'ThemeProvider & Usage',
          content: 'src/theme/docs/ThemeProvider.md',
        },
        {
          name: 'Fonts',
          content: 'src/theme/docs/Fonts.md',
        },
        {
          name: 'Colors',
          content: 'src/theme/docs/Colors.md',
        },
        {
          name: 'Color Utilities',
          content: 'src/theme/docs/Utilities.md',
        },
      ],
    },
  ],
  usageMode: 'collapse',
  styleguideComponents: {
    Wrapper: path.join(__dirname, 'docs/StyleGuideWrapper'),
  },
  styleguideDir: 'docs/styleguide',
  title: 'Graylog UI documentation',
  webpackConfig: {
    module: webpackConfig.module,
    resolve: webpackConfig.resolve,
    resolveLoader: webpackConfig.resolveLoader,
  },
};
