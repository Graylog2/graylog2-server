// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'enterprise/components/aggregationbuilder/AggregationBuilder';

import { generateSeries } from '../Series';
import transformKeys from '../TransformKeys';
import XYPlot from '../XYPlot';

const LineVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => (
  <XYPlot config={config}
          chartData={generateSeries(transformKeys(config.rowPivots, config.columnPivots, data), 'scatter')} />
);

LineVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

LineVisualization.type = 'line';

export default LineVisualization;
