import React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import GenericPlot from '../GenericPlot';
import { generateSeries } from '../Series';

const maxItemsPerRow = 4;

const _verticalDimensions = (idx, total) => {
  const rows = Math.ceil(total / maxItemsPerRow);
  const position = Math.floor(idx / maxItemsPerRow);

  const sliceSize = 1 / rows;
  const spacer = sliceSize * 0.1;
  return [(sliceSize * position) + spacer, (sliceSize * (position + 1)) - spacer];
};

const _horizontalDimensions = (idx, total) => {
  const position = idx % maxItemsPerRow;

  const sliceSize = 1 / Math.min(total, maxItemsPerRow);
  const spacer = sliceSize * 0.1;
  return [(sliceSize * position) + spacer, (sliceSize * (position + 1)) - spacer];
};

const _generateSeries = (data) => {
  return generateSeries(data, 'pie', (type, name, x, y, idx, total) => ({
    type,
    name,
    hole: 0.4,
    labels: x,
    values: y,
    domain: {
      x: _horizontalDimensions(idx, total),
      y: _verticalDimensions(idx, total),
    },
  }));
};

const PieVisualization = ({ data }) => <GenericPlot chartData={_generateSeries(data)} />;

PieVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default PieVisualization;
