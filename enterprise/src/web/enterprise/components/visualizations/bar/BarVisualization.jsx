import React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import GenericPlot from '../GenericPlot';
import { generateSeries } from '../Series';
import transformKeys from '../TransformKeys';

const BarVisualization = ({ config, data }) => <GenericPlot chartData={generateSeries(transformKeys(config.rowPivots, config.columnPivots, data), 'bar')} />;

BarVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

BarVisualization.type = 'bar';

export default BarVisualization;
