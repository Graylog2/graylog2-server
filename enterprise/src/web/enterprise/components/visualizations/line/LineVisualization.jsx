import React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import normalizeRows from 'enterprise/logic/NormalizeRows';
import GenericPlot from '../GenericPlot';
import { generateSeries } from '../Series';

const _generateSeries = (config, data) => {
  const results = normalizeRows(config.rowPivots.slice(), config.series, data, true);
  return generateSeries(config, results, 'scatter');
};

const LineVisualization = ({ config, data }) => <GenericPlot chartData={_generateSeries(config, data[0].results)} />;

LineVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default LineVisualization;

