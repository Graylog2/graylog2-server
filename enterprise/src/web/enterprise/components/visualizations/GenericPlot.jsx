import React from 'react';
import PropTypes from 'prop-types';

import Plotly from 'enterprise/custom-plotly';
import createPlotlyComponent from 'react-plotly.js/factory';

const Plot = createPlotlyComponent(Plotly);

const GenericPlot = ({ chartData }) => (
  <Plot data={chartData}
        style={{ position: 'absolute' }}
        fit
        layout={{
          margin: {
            t: 10,
            pad: 10,
          },
        }}
        config={{ displayModeBar: false }}/>
);

GenericPlot.propTypes = {
  chartData: PropTypes.object.isRequired,
};

export default GenericPlot;
