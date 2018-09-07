import React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import GenericPlot from '../GenericPlot';
import { generateSeries } from '../Series';
import transformKeys from '../TransformKeys';

const seriesGenerator = (type, name, labels, values) => ({ type, name, x: labels, y: values, mode: 'markers' });
const ScatterVisualization = ({ config, data }) => <GenericPlot chartData={generateSeries(transformKeys(config.rowPivots, config.columnPivots, data), 'scatter', seriesGenerator)} />;

ScatterVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

ScatterVisualization.type = 'scatter';

export default ScatterVisualization;
