// @flow strict
import React, { useCallback } from 'react';
import PropTypes from 'prop-types';
import chroma from 'chroma-js';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import toPlotly from 'views/logic/aggregationbuilder/visualizations/Interpolation';
import EventHandler from 'views/logic/searchtypes/events/EventHandler';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';

import type { ChartDefinition } from '../ChartData';
import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';
import ViewColorContext from '../../contexts/ViewColorContext';

const getCurrentChartColor = (fullData, name) => {
  const data = fullData.find((d) => (d.name === name));
  if (data && data.line && data.line.color) {
    const { line: { color } } = data;
    return color;
  }
  return undefined;
};

const noColors = 40;
const scale = chroma.scale(['#fafa6e','#2A4858']).colors(40);
const randomButStaticColorFor = (name) => {
  const sum = name.split('').map((c) => c.charCodeAt(0)).reduce((prev, cur) => prev + cur, 0);
  return scale[sum % noColors];
};

const getPinnedChartColor = (chart, getColor) => ({ line: { color: getColor(chart.name) } });

const LineVisualization: VisualizationComponent = makeVisualization(({ config, data, effectiveTimerange, height }: VisualizationComponentProps) => {
  // $FlowFixMe: We need to assume it is a LineVisualizationConfig instance
  const visualizationConfig: LineVisualizationConfig = config.visualizationConfig ?? LineVisualizationConfig.empty();
  const { interpolation = 'linear' } = visualizationConfig;
  const chartGenerator = useCallback((type, name, labels, values): ChartDefinition => ({
    type,
    name,
    x: labels,
    y: values,
    line: { shape: toPlotly(interpolation) },
  }), [interpolation]);

  const chartDataResult = chartData(config, data.chart || Object.values(data)[0], 'scatter', chartGenerator);
  const layout = {};
  if (config.eventAnnotation && data.events) {
    const { eventChartData, shapes } = EventHandler.toVisualizationData(data.events, config.formattingSettings);
    chartDataResult.push(eventChartData);
    layout.shapes = shapes;
  }

  return (
    <XYPlot config={config}
            plotLayout={layout}
            effectiveTimerange={effectiveTimerange}
            getCurrentChartColor={getCurrentChartColor}
            height={height}
            getPinnedChartColor={getPinnedChartColor}
            chartData={chartDataResult} />
  );
}, 'line');

LineVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
  height: PropTypes.number,
};

export default LineVisualization;
