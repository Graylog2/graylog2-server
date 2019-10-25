// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { ChartDefinition } from '../ChartData';
import GenericPlot from '../GenericPlot';

import { chartData } from '../ChartData';

const _seriesGenerator = (type, name, labels, values, z): ChartDefinition => ({ type, name, x: labels, y: values, z, connectgaps: true });

const HeatmapVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => {
  return (
    <GenericPlot chartData={chartData(config, data, 'heatmap', _seriesGenerator)} />
  );
};

HeatmapVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

HeatmapVisualization.type = 'heatmap';

export default HeatmapVisualization;
