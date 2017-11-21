/* This file contains configuration for React Styleguidist https://react-styleguidist.js.org/ */
const webpackConfig = require('./webpack.config.js');

module.exports = {
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
    {
      name: 'Util',
      components: 'src/util/[A-Z]*.jsx?',
    },
  ],
  showUsage: true,
  styleguideDir: 'docs/styleguide',
  title: 'Graylog components',
  webpackConfig: {
    module: webpackConfig.module,
    resolve: webpackConfig.resolve,
    resolveLoader: webpackConfig.resolveLoader,
  },
};
