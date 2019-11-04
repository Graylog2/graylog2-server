// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { values, merge, fill, find, isEmpty, get } from 'lodash';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { ChartDefinition, ExtractedSeries } from '../ChartData';

import GenericPlot from '../GenericPlot';
import { chartData } from '../ChartData';

const COLORSCALE = [
  [0.00, '#440154'],
  [0.05, '#481567'],
  [0.10, '#483677'],
  [0.15, '#453781'],
  [0.20, '#404788'],
  [0.30, '#39568c'],
  [0.35, '#33638d'],
  [0.40, '#2d708e'],
  [0.45, '#287d8e'],
  [0.50, '#238a8d'],
  [0.55, '#1f968b'],
  [0.60, '#20a387'],
  [0.65, '#29af7f'],
  [0.70, '#3cbb75'],
  [0.75, '#55c667'],
  [0.80, '#73d055'],
  [0.85, '#95d840'],
  [0.90, '#b8de29'],
  [0.95, '#dce319'],
  [1.00, '#fde725'],
];

const _generateSeries = (type, name, x, y, z, idx, total, config): ChartDefinition => {
  console.log(config);
  const xAxisTitle = get(config, '_value.rowPivots[0].field');
  const yAxisTitle = get(config, '_value.columnPivots[0].field');
  const seriesTitle = get(config, '_value.series[0]._value.function');
  return {
    type,
    name,
    x: y,
    y: x,
    z,
    customdata: z,
    hovertemplate: `${xAxisTitle}: %{x}<br>${yAxisTitle}: %{y}<br>${seriesTitle}: %{customdata}<extra></extra>`,
    colorscale: COLORSCALE,
  };
};

const _fillEmptyFields = (z: Array<Array<any>>, xLabels: Array<any>) => {
  const defaultValue = 'None';
  return z.map((series) => {
    const newSeries = fill(Array(xLabels.length), defaultValue);
    return merge(newSeries, series);
  });
};

const _transposeMatrix = (z: Array<Array<any>>) => {
  if (isEmpty(z)) {
    return z;
  }
  return z[0].map((_, c) => { return z.map((r) => { return r[c]; }); });
};

const _formatSeries = ({ valuesBySeries, xLabels }: {valuesBySeries: Object, xLabels: Array<any>}): ExtractedSeries => {
  // When using the hovertemplate, we need to provie a value for empty z values.
  // Otherwise plotly would throw errors when hovering over a field.
  // We need to transpose the z matrix, because we are changing the x and y label in the generator function
  const z = _transposeMatrix(_fillEmptyFields(values(valuesBySeries), xLabels));
  const yLabels = Object.keys(valuesBySeries);
  return [[
    'Heatmap Chart',
    xLabels,
    yLabels,
    z,
  ]];
};

const _chartLayout = (heatmapData) => {
  const axisConfig = {
    type: undefined,
    fixedrange: true,
  };
  // Adding the axis type, without provided z data, would hide the empty grid.
  if (find(heatmapData, series => !isEmpty(series.z))) {
    axisConfig.type = 'category';
  }
  return {
    yaxis: axisConfig,
    xaxis: axisConfig,
    margin: {
      b: 80,
      l: 80,
    },
    plot_bgcolor: '#440154',
  };
};

const _leafSourceMatcher = ({ source }) => source.endsWith('leaf') && source !== 'row-leaf';

const HeatmapVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => {
  const heatmapData = chartData(config, data, 'heatmap', _generateSeries, _formatSeries, _leafSourceMatcher);
  const layout = _chartLayout(heatmapData);
  return (
    <GenericPlot chartData={heatmapData} layout={layout} />
  );
};

HeatmapVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

HeatmapVisualization.type = 'heatmap';

export default HeatmapVisualization;
