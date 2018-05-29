import React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import GenericPlot from '../GenericPlot';
import { generateSeries } from '../Series';

const BarVisualization = ({ config, data }) => <GenericPlot chartData={generateSeries(data, 'bar')} />;

BarVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default BarVisualization;
