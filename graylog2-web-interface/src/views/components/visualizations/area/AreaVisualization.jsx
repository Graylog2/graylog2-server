// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import type { ChartDefinition } from '../ChartData';
import type { VisualizationComponent, VisualizationComponentProps } from '../../aggregationbuilder/AggregationBuilder';
import { AggregationType } from '../../aggregationbuilder/AggregationBuilderPropTypes';
import XYPlot from '../XYPlot';
import { chartData } from '../ChartData';

const chartGenerator = (type, name, labels, values): ChartDefinition => ({
  type,
  name,
  x: labels,
  y: values,
  fill: 'tozeroy',
  line: { shape: 'linear' },
});

const getChartColor = (fullData, name) => {
  const data = fullData.find(d => (d.name === name));
  if (data && data.line && data.line.color) {
    const { line: { color } } = data;
    return color;
  }
  return undefined;
};

const setChartColor = (chart, colors) => ({ line: { color: colors[chart.name] } });

const AreaVisualization: VisualizationComponent = ({ config, data, effectiveTimerange }: VisualizationComponentProps) => (
  <XYPlot config={config}
          effectiveTimerange={effectiveTimerange}
          getChartColor={getChartColor}
          setChartColor={setChartColor}
          chartData={chartData(config, data.chart || Object.values(data)[0], 'scatter', chartGenerator)} />
);

AreaVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.objectOf(PropTypes.arrayOf(PropTypes.object)).isRequired,
};

AreaVisualization.type = 'area';

export default AreaVisualization;
