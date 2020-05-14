// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type {
  VisualizationComponent,
  VisualizationComponentProps,
} from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';

import GenericPlot from '../GenericPlot';
import { chartData } from '../ChartData';

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

const _generateSeries = (type, name, x, y, z, idx, total) => ({
  type,
  name,
  hole: 0.4,
  labels: x,
  values: y,
  domain: {
    x: _horizontalDimensions(idx, total),
    y: _verticalDimensions(idx, total),
  },
});

const getCurrentChartColor = (fullDataArray, name) => {
  const fullData = fullDataArray.find((d) => d.labels.indexOf(name) >= 0);
  if (fullData && fullData.labels && fullData.marker && fullData.marker.colors) {
    const indexOfName = fullData.labels.indexOf(name);
    // $FlowFixMe the check above ensures the presents of marker
    const { marker: { colors } } = fullData;

    // $FlowFixMe the check above ensures the presents of colors
    return colors[indexOfName];
  }
  return undefined;
};

const getPinnedChartColor = (chart, colorMap) => {
  const colors = chart.labels.map((label) => colorMap[label]);
  return { marker: { colors } };
};

const PieVisualization: VisualizationComponent = makeVisualization(({ config, data }: VisualizationComponentProps) => (
  <GenericPlot chartData={chartData(config, data.chart || Object.values(data)[0], 'pie', _generateSeries)}
               getCurrentChartColor={getCurrentChartColor}
               getPinnedChartColor={getPinnedChartColor} />
), 'pie');

PieVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default PieVisualization;
