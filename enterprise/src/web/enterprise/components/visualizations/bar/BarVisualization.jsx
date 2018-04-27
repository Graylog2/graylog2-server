import React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import normalizeRows from 'enterprise/logic/NormalizeRows';
import GenericPlot from '../GenericPlot';
import { generateSeries } from '../Series';

const _generateSeries = (config, data) => {
  const rowPivots = config.rowPivots.map(({ field }) => field);
  const columnPivots = config.columnPivots.map(({ field }) => field);
  
  const results = normalizeRows(rowPivots, columnPivots, config.series, data, true);
  return generateSeries(config, results, 'bar');
};

const BarVisualization = ({ config, data }) => <GenericPlot chartData={_generateSeries(config, data[0].results)} />;

BarVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default BarVisualization;
