// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { values, merge, fill, find, isEmpty, get } from 'lodash';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { ChartDefinition, ExtractedSeries } from '../ChartData';

import GenericPlot from '../GenericPlot';
import { chartData } from '../ChartData';

const _generateSeries = (type, name, x, y, z, idx, total, rowPivots, columnPivots, series): ChartDefinition => {
  const xAxisTitle = get(rowPivots, '[0].field');
  const yAxisTitle = get(columnPivots, '[0].field');
  const seriesTitle = get(series, '[0]._value.function');
  return {
    type,
    name,
    test: 'wtwe',
    x: y,
    y: x,
    z,
    transpose: true,
    hovertemplate: `${xAxisTitle}: %{x}<br>${yAxisTitle}: %{y}<br>${seriesTitle}: %{z}<extra></extra>`,
  };
};

const _formatSeries = ({ valuesBySeries, xLabels }: {valuesBySeries: Object, xLabels: Array<any>}): ExtractedSeries => {
  // When using the hovertemplate, the value z can't be undefined. Plotly would throw errors when hovering over a field.
  const z = values(valuesBySeries).map((series) => {
    const newSeries = fill(Array(xLabels.length), null);
    return merge(newSeries, series);
  });
  const yLabels = Object.keys(valuesBySeries);
  return [[
    'Heatmap Chart',
    xLabels,
    yLabels,
    z,
  ]];
};

const _chartLayout = (heatmapData, config) => {
  const seriesTitle = get(config, '_value.series[0]._value.function') || '';
  const isCount = seriesTitle.startsWith('count') || seriesTitle.startsWith('sum');
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
    plot_bgcolor: isCount ? 'rgb(8, 17, 164)' : 'transparent',
  };
};

const _leafSourceMatcher = ({ source }) => source.endsWith('leaf') && source !== 'row-leaf';

const HeatmapVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => {
  const heatmapData = chartData(config, data, 'heatmap', _generateSeries, _formatSeries, _leafSourceMatcher);
  const layout = _chartLayout(heatmapData, config);
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
