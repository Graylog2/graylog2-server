/* This file contains configuration for React Styleguidist https://react-styleguidist.js.org/ */
const webpackConfig = require('./webpack.config.js');

module.exports = {
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
  showUsage: true,
  styleguideDir: 'docs/styleguide',
  title: 'Graylog UI documentation',
  webpackConfig: {
    module: webpackConfig.module,
    resolve: webpackConfig.resolve,
    resolveLoader: webpackConfig.resolveLoader,
  },
};
