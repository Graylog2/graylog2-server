import React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import GenericPlot from '../GenericPlot';
import { generateSeries } from '../Series';
import { transformKeys } from '../TransformKeys';

const LineVisualization = ({ config, data }) => <GenericPlot chartData={generateSeries(transformKeys(config.rowPivots, config.columnPivots, data), 'scatter')} />;

LineVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

LineVisualization.type = 'line';

export default LineVisualization;

