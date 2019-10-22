// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { ChartDefinition } from '../ChartData';

import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';

const chartGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values });

const HeatmapVisualization: VisualizationComponent = ({ config, data, effectiveTimerange }: VisualizationComponentProps) => {
  return (
    <XYPlot config={config}
            effectiveTimerange={effectiveTimerange}
            chartData={chartData(config, data, 'heatmap', chartGenerator)} />
  );
};

HeatmapVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

HeatmapVisualization.type = 'heatmap';

export default HeatmapVisualization;
