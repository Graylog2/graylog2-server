// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';

import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';

const seriesGenerator = (type, name, labels, values) => ({ type, name, x: labels, y: values, mode: 'markers' });

const ScatterVisualization: VisualizationComponent = ({ config, data, effectiveTimerange }: VisualizationComponentProps) => (
  <XYPlot config={config}
          chartData={chartData(config, data.chart || Object.values(data[0]), 'scatter', seriesGenerator)}
          effectiveTimerange={effectiveTimerange} />
);

ScatterVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

ScatterVisualization.type = 'scatter';

export default ScatterVisualization;
