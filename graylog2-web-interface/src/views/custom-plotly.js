import Plotly from 'plotly.js/lib/core';
import Bar from 'plotly.js/lib/bar';
import Pie from 'plotly.js/lib/pie';
import Scatter from 'plotly.js/lib/scatter';

Plotly.register([
  Bar,
  Pie,
  Scatter,
]);

export default Plotly;
