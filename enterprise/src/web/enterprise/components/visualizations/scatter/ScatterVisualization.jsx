// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'enterprise/components/aggregationbuilder/AggregationBuilder';

import { generateSeries } from '../Series';
import transformKeys from '../TransformKeys';
import XYPlot from '../XYPlot';

const seriesGenerator = (type, name, labels, values) => ({ type, name, x: labels, y: values, mode: 'markers' });

const ScatterVisualization: VisualizationComponent = ({ config, data }: VisualizationComponentProps) => (
  <XYPlot config={config}
          chartData={generateSeries(transformKeys(config.rowPivots, config.columnPivots, data), 'scatter', seriesGenerator)} />
);

ScatterVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

ScatterVisualization.type = 'scatter';

export default ScatterVisualization;
