const Plotly = require('plotly.js/lib/core');

// Load in the trace types for pie, and choropleth
Plotly.register([
  /* eslint-disable global-require */
  require('plotly.js/lib/bar'),
  require('plotly.js/lib/pie'),
  require('plotly.js/lib/scatter'),
  /* eslint-enable global-require */
]);

module.exports = Plotly;
