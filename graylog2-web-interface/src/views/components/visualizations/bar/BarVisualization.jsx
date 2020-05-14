// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import EventHandler from 'views/logic/searchtypes/events/EventHandler';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';

import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';
import BarVisualizationConfig from '../../../logic/aggregationbuilder/visualizations/BarVisualizationConfig';

type ChartDefinition = {
  type: string,
  name: string,
  x?: Array<string>,
  y?: Array<any>,
  z?: Array<Array<any>>,
  opacity: number,
};

const getCurrentChartColor = (fullData, name) => {
  const data = fullData.find((d) => (d.name === name)).marker;
  if (data && data.marker && data.marker.color) {
    const { marker: color } = data;
    return color;
  }
  return undefined;
};

const getPinnedChartColor = (chart, colors) => ({ marker: { color: colors[chart.name] } });

const BarVisualization: VisualizationComponent = makeVisualization(({ config, data, effectiveTimerange, height }: VisualizationComponentProps) => {
  const { visualizationConfig } = config;
  const layout = {};

  /* $FlowFixMe: type inheritance does not work here */
  const barVisualizationConfig: BarVisualizationConfig = visualizationConfig;

  if (barVisualizationConfig?.barmode) {
    layout.barmode = barVisualizationConfig.barmode;
  }
  const opacity = barVisualizationConfig?.opacity ?? 1.0;

  const _seriesGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values, opacity });

  const rows = data.chart || Object.values(data)[0];
  const chartDataResult = chartData(config, rows, 'bar', _seriesGenerator);
  if (config.eventAnnotation && data.events) {
    const { eventChartData, shapes } = EventHandler.toVisualizationData(data.events, config.formattingSettings);
    chartDataResult.push(eventChartData);
    layout.shapes = shapes;
  }

  return (
    <XYPlot config={config}
            chartData={chartDataResult}
            effectiveTimerange={effectiveTimerange}
            getCurrentChartColor={getCurrentChartColor}
            height={height}
            getPinnedChartColor={getPinnedChartColor}
            plotLayout={layout} />
  );
}, 'bar');

BarVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.object.isRequired,
  height: PropTypes.number,
};

export default BarVisualization;
