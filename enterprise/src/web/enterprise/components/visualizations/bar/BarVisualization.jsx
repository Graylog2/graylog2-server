// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'enterprise/components/aggregationbuilder/AggregationBuilder';

import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';

const BarVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => (
  <XYPlot config={config}
          chartData={chartData(config, data, 'bar')} />
);

BarVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

BarVisualization.type = 'bar';

export default BarVisualization;
