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
import * as React from 'react';
import { useMemo } from 'react';
import PropTypes from 'prop-types';

import type { Shapes } from 'views/logic/searchtypes/events/EventHandler';
import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';
import ScatterVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/ScatterVisualizationConfig';
import type { Generator } from 'views/components/visualizations/ChartData';
import type { ChartConfig } from 'views/components/visualizations/GenericPlot';
import type ColorMapper from 'views/components/visualizations/ColorMapper';

import XYPlot from '../XYPlot';

const seriesGenerator: Generator = ({ type, name, labels, values }) => ({ type, name, x: labels, y: values, mode: 'markers' });

const setChartColor = (chart: ChartConfig, colors: ColorMapper) => ({ marker: { color: colors.get(chart.name) } });

const ScatterVisualization = makeVisualization(({
  config,
  data,
  effectiveTimerange,
  height,
}: VisualizationComponentProps) => {
  const visualizationConfig = (config.visualizationConfig ?? ScatterVisualizationConfig.empty()) as ScatterVisualizationConfig;
  const rows = useMemo(() => retrieveChartData(data), [data]);
  const _chartDataResult = useChartData(rows, {
    widgetConfig: config,
    chartType: 'scatter',
    generator: seriesGenerator,
  });
  const { eventChartData, shapes } = useEvents(config, data.events);

  const chartDataResult = eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult;
  const layout: { shapes?: Shapes } = shapes ? { shapes } : {};

  return (
    <XYPlot config={config}
            axisType={visualizationConfig.axisType}
            chartData={chartDataResult}
            setChartColor={setChartColor}
            plotLayout={layout}
            height={height}
            effectiveTimerange={effectiveTimerange} />
  );
}, 'scatter');

ScatterVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: AggregationResult.isRequired,
  height: PropTypes.number,
};

export default ScatterVisualization;
