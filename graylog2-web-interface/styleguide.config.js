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
const fs = require('fs');
const path = require('path');

const requireIt = require('react-styleguidist/lib/loaders/utils/requireIt').default;
const docGenTSParse = require('react-docgen-typescript')
  .withCustomConfig(`${process.cwd()}/tsconfig.json`, {})
  .parse;
const docGenParse = require('react-docgen').parse;

const webpackConfig = require('./webpack.config.js');

module.exports = {
  require: [
    'core-js/stable',
    'regenerator-runtime/runtime',
    'bootstrap/less/bootstrap.less',
    'toastr/toastr.less',
    'stylesheets/typeahead.less',
  ],
  propsParser: (filePath, source, resolver, handlers) => {
    return path.parse(filePath).ext === '.tsx'
      ? docGenTSParse(filePath)
      : docGenParse(source, resolver, handlers, { envName: 'docs', filename: filePath });
  },
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
        {
          name: 'Spacings',
          content: 'src/theme/docs/Spacings.md',
        },
      ],
    },
    {
      name: 'Shared Components',
      sections: [
        {
          name: 'Bootstrap',
          components: 'src/components/bootstrap/[A-Z]!(*.example)*.{jsx,tsx}',
        },
        {
          name: 'Common',
          components: 'src/components/common/[A-Z]*.jsx',
        },
        {
          name: 'Configuration Forms',
          components: 'src/components/configurationforms/[A-Z]*.{jsx,tsx}',
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
  getExampleFilename(componentPath) {
    const pathMatch = componentPath.match(/\.[t|j]sx?$/);

    if (pathMatch) {
      if (pathMatch[0] === '.tsx') {
        const tsxPath = componentPath.replace(/\.tsx?$/, '.example.tsx');
        const mdPath = componentPath.replace(/\.tsx?$/, '.md');

        if (fs.existsSync(tsxPath)) {
          return tsxPath;
        }

        if (fs.existsSync(mdPath)) {
          return mdPath;
        }

        return componentPath;
      }

      return componentPath.replace(/\.jsx?$/, '.md');
    }

    return componentPath;
  },
  updateDocs(docs) {
    if (!docs.filePath) {
      return docs;
    }

    const updatedDocs = JSON.parse(JSON.stringify(docs));

    const loader = path.join(__dirname, 'webpack/examples-loader.js');
    const requirePath = updatedDocs.examples.require.replace(/^!!([^!]*)!/, `!!${loader}!`);
    updatedDocs.examples = requireIt(requirePath);

    return updatedDocs;
  },
  webpackConfig: {
    module: webpackConfig.module,
    resolve: webpackConfig.resolve,
    resolveLoader: webpackConfig.resolveLoader,
  },
};
