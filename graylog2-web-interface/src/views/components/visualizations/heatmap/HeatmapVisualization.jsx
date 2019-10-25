// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { ChartDefinition, ExtractedSeries } from '../ChartData';

import GenericPlot from '../GenericPlot';
import { chartData } from '../ChartData';


const _seriesGenerator = (type, name, labels, values, z): ChartDefinition => ({ type, name, x: labels, y: values, z, connectgaps: true });

const _formatSeriesXYZ = (valuesBySeries: Object, xLabels: Array<any>): ExtractedSeries => {
  const y = Object.keys(valuesBySeries);
  const z = Object.values(valuesBySeries);
  return [[
    'XYZ Chart',
    xLabels,
    y,
    z,
  ]];
};


const HeatmapVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => {
  const showColSummary = false;
  return (
    <GenericPlot chartData={chartData(config, data, 'heatmap', _seriesGenerator, _formatSeriesXYZ, showColSummary)} />
  );
};

HeatmapVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

HeatmapVisualization.type = 'heatmap';

export default HeatmapVisualization;
