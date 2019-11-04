// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { values, find, isEmpty } from 'lodash';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { Value } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { ChartDefinition, ExtractedSeries } from '../ChartData';

import GenericPlot from '../GenericPlot';
import { chartData } from '../ChartData';

const _generateSeries = (type, name, x, y, z): ChartDefinition => ({ type, name, x: y, y: x, z, transpose: true });

const _formatSeries = ({ valuesBySeries, xLabels }: {valuesBySeries: Object, xLabels: Array<any>}): ExtractedSeries => {
  const z = values(valuesBySeries);
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
  };
};

const _leafSourceMatcher = (value: Value) => value.source.endsWith('leaf') && value.source !== 'row-leaf';

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
