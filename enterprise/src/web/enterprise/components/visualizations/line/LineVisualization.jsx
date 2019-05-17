// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'enterprise/components/aggregationbuilder/AggregationBuilder';
import type { ChartDefinition } from '../ChartData';

import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';

const chartGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values, line: { shape: 'linear' } });

const LineVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => (
  <XYPlot config={config}
          chartData={chartData(config, data, 'scatter', chartGenerator)} />
);

LineVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

LineVisualization.type = 'line';

export default LineVisualization;
