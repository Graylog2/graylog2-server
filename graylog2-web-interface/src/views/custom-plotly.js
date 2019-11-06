import Plotly from 'plotly.js/lib/core';
import Bar from 'plotly.js/lib/bar';
import Pie from 'plotly.js/lib/pie';
import Heatmap from 'plotly.js/lib/heatmap';
import Scatter from 'plotly.js/lib/scatter';

Plotly.register([
  Bar,
  Pie,
  Scatter,
  Heatmap,
]);

export default Plotly;
