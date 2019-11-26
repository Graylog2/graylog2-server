// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { ChartDefinition } from '../ChartData';

import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';

const chartGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values, line: { shape: 'linear' } });

const getChartColor = (fullData, name) => {
  const data = fullData.find(d => (d.name === name));
  if (data && data.line && data.line.color) {
    const { line: { color } } = data;
    return color;
  }
  return undefined;
};

const setChartColor = (chart, colors) => ({ line: { color: colors[chart.name] } });

const LineVisualization: VisualizationComponent = ({ config, data, effectiveTimerange }: VisualizationComponentProps) => (
  <XYPlot config={config}
          effectiveTimerange={effectiveTimerange}
          getChartColor={getChartColor}
          setChartColor={setChartColor}
          chartData={chartData(config, data.chart || Object.values(data)[0], 'scatter', chartGenerator)} />
);

LineVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

LineVisualization.type = 'line';

export default LineVisualization;
