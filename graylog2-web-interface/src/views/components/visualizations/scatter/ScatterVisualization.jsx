// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import EventHandler from 'views/logic/searchtypes/events/EventHandler';
import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';

import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';

const seriesGenerator = (type, name, labels, values) => ({ type, name, x: labels, y: values, mode: 'markers' });

const getCurrentChartColor = (fullData, name) => {
  const data = fullData.find((d) => (d.name === name));
  if (data && data.line && data.line.color) {
    const { line: { color } } = data;
    return color;
  }
  return undefined;
};

const getPinnedChartColor = (chart, getColor) => ({ line: { color: getColor(chart.name) } });

const ScatterVisualization: VisualizationComponent = makeVisualization(({ config, data, effectiveTimerange, height }: VisualizationComponentProps) => {
  const chartDataResult = chartData(config, data.chart || Object.values(data)[0], 'scatter', seriesGenerator);
  const layout = {};
  if (config.eventAnnotation && data.events) {
    const { eventChartData, shapes } = EventHandler.toVisualizationData(data.events, config.formattingSettings);
    chartDataResult.push(eventChartData);
    layout.shapes = shapes;
  }
  return (
    <XYPlot config={config}
            chartData={chartDataResult}
            plotLayout={layout}
            height={height}
            effectiveTimerange={effectiveTimerange}
            getCurrentChartColor={getCurrentChartColor}
            getPinnedChartColor={getPinnedChartColor} />
  );
}, 'scatter');

ScatterVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
  height: PropTypes.number,
};

export default ScatterVisualization;
