import React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import { generateSeries } from '../Series';
import transformKeys from '../TransformKeys';
import XYPlot from '../XYPlot';

const LineVisualization = ({ config, data }) => (
  <XYPlot config={config}
          chartData={generateSeries(transformKeys(config.rowPivots, config.columnPivots, data), 'scatter')} />
);

LineVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

LineVisualization.type = 'line';

export default LineVisualization;

