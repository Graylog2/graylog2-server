// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { chunk, flatten, values } from 'lodash';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { ChartDefinition, ExtractedSeries } from '../ChartData';

import GenericPlot from '../GenericPlot';
import { chartData } from '../ChartData';

const _generateSeries = (type, name, x, y, z): ChartDefinition => ({ type, name, x, y, z, transpose: true });

const _formatSeries = ({ valuesBySeries, xLabels }: {valuesBySeries: Object, xLabels: Array<any>}): ExtractedSeries => {
  const z = values(valuesBySeries);
  const transposedX = chunk(Object.keys(valuesBySeries));
  const transposedY = flatten(xLabels);
  return [[
    'Heatmap Chart',
    transposedX,
    transposedY,
    z,
  ]];
};

const HeatmapVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => {
  const showSummaryCol = false;
  const heatmapData = chartData(config, data, 'heatmap', _generateSeries, _formatSeries, showSummaryCol);
  const layout = { yaxis: { type: 'category' }, xaxis: { type: 'category' } };
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
