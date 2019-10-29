// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { values } from 'lodash';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
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

const HeatmapVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => {
  const showSummaryCol = false;
  const heatmapData = chartData(config, data, 'heatmap', _generateSeries, _formatSeries, showSummaryCol);
  const axisConfig = {
    type: 'category',
    fixedrange: true,
  };
  const layout = {
    yaxis: axisConfig,
    xaxis: axisConfig,
    margin: {
      b: 80,
      l: 80,
    },
  };
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
