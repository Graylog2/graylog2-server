/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React, { useCallback } from 'react';
import PropTypes from 'prop-types';

import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import toPlotly from 'views/logic/aggregationbuilder/visualizations/Interpolation';
import EventHandler, { Shapes } from 'views/logic/searchtypes/events/EventHandler';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';

import type { ChartDefinition } from '../ChartData';
import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';

const getChartColor = (fullData, name) => {
  const data = fullData.find((d) => (d.name === name));

  if (data && data.line && data.line.color) {
    const { line: { color } } = data;

    return color;
  }

  return undefined;
};

const setChartColor = (chart, colors) => ({ line: { color: colors.get(chart.name) } });

const LineVisualization = makeVisualization(({ config, data, effectiveTimerange, height }: VisualizationComponentProps) => {
  const visualizationConfig = (config.visualizationConfig || LineVisualizationConfig.empty()) as LineVisualizationConfig;
  const { interpolation = 'linear' } = visualizationConfig;
  const chartGenerator = useCallback((type, name, labels, values): ChartDefinition => ({
    type,
    name,
    x: labels,
    y: values,
    line: { shape: toPlotly(interpolation) },
  }), [interpolation]);

  const chartDataResult = chartData(config, data.chart || Object.values(data)[0], 'scatter', chartGenerator);
  const layout: { shapes?: Shapes } = {};

  if (config.eventAnnotation && data.events) {
    const { eventChartData, shapes } = EventHandler.toVisualizationData(data.events, config.formattingSettings);

    chartDataResult.push(eventChartData);
    layout.shapes = shapes;
  }

  return (
    <XYPlot config={config}
            plotLayout={layout}
            effectiveTimerange={effectiveTimerange}
            getChartColor={getChartColor}
            height={height}
            setChartColor={setChartColor}
            chartData={chartDataResult} />
  );
}, 'line');

LineVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: AggregationResult.isRequired,
  height: PropTypes.number,
};

export default LineVisualization;
