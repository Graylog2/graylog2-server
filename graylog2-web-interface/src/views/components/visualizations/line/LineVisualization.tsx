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
import React, { useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';

import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import toPlotly from 'views/logic/aggregationbuilder/visualizations/Interpolation';
import type { Shapes } from 'views/logic/searchtypes/events/EventHandler';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';

import type { ChartDefinition } from '../ChartData';
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

const LineVisualization = makeVisualization(({
  config,
  data,
  effectiveTimerange,
  height,
}: VisualizationComponentProps) => {
  const visualizationConfig = (config.visualizationConfig ?? LineVisualizationConfig.empty()) as LineVisualizationConfig;
  const { interpolation = 'linear' } = visualizationConfig;
  const chartGenerator = useCallback((type, name, labels, values): ChartDefinition => ({
    type,
    name,
    x: labels,
    y: values,
    line: { shape: toPlotly(interpolation) },
  }), [interpolation]);

  const rows = useMemo(() => retrieveChartData(data), [data]);
  const _chartDataResult = useChartData(rows, {
    widgetConfig: config,
    chartType: 'scatter',
    generator: chartGenerator,
  });

  const { eventChartData, shapes } = useEvents(config, data.events);

  const chartDataResult = eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult;
  const layout: { shapes?: Shapes } = shapes ? { shapes } : {};

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
