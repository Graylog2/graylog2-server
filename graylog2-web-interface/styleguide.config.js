/* This file contains configuration for React Styleguidist https://react-styleguidist.js.org/ */
const path = require('path');
const webpackConfig = require('./webpack.config.js');

module.exports = {
  require: [
    'bootstrap/less/bootstrap.less',
    'font-awesome/css/font-awesome.css',
    'opensans-npm-webfont',
    'stylesheets/bootstrap-submenus.less',
    'toastr/toastr.less',
    'rickshaw/rickshaw.css',
    'stylesheets/typeahead.less',
    'injection/builtins.js',
  ],
  sections: [
    {
      name: 'Introduction',
      content: 'docs/introduction.md',
    },
    {
      name: 'Style guide',
      content: 'docs/styleguide.md',
    },
    {
      name: 'Theming Details',
      sections: [
        {
          name: 'ThemeProvider',
          content: 'src/theme/docs/ThemeProvider.md',
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
    {
      name: 'Documentation',
      content: 'docs/documentation.md',
    },
    {
      name: 'Tests',
      content: 'docs/tests.md',
    },
    {
      name: 'Shared Components',
      sections: [
        {
          name: 'Bootstrap',
          components: 'src/components/bootstrap/[A-Z]*.jsx',
        },
        {
          name: 'Common',
          components: 'src/components/common/[A-Z]*.jsx',
        },
        {
          name: 'Themeable',
          components: 'src/components/graylog/[A-Z]*.jsx',
        },
        {
          name: 'Configuration Forms',
          components: 'src/components/configurationforms/[A-Z]*.jsx',
        },
        {
          name: 'Inputs',
          components: 'src/components/inputs/[A-Z]*.jsx',
        },
        {
          name: 'Visualizations',
          components: 'src/components/visualizations/[A-Z]*.jsx',
        },
      ],
    },
    {
      name: 'Util objects',
      content: 'docs/util-objects.md',
    },
  ],
  usageMode: 'expand',
  styleguideComponents: {
    Wrapper: path.join(__dirname, 'src/theme/GraylogThemeContext'),
  },
  styleguideDir: 'docs/styleguide',
  title: 'Graylog UI documentation',
  webpackConfig: {
    module: webpackConfig.module,
    resolve: webpackConfig.resolve,
    resolveLoader: webpackConfig.resolveLoader,
  },
};
