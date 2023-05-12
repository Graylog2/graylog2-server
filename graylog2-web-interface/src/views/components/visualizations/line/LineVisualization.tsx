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
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';
import { DEFAULT_AXIS_TYPE } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import { keySeparator, humanSeparator } from 'views/Constants';

import type { Generator } from '../ChartData';
import XYPlot from '../XYPlot';

const getChartColor = (fullData, name) => {
  const data = fullData.find((d) => (d.name === name));

  if (data && data.line && data.line.color) {
    const { line: { color } } = data;

    return color;
  }

  return undefined;
};

const LineVisualization = makeVisualization(({
  config,
  data,
  effectiveTimerange,
  height,
}: VisualizationComponentProps) => {
  const visualizationConfig = (config.visualizationConfig ?? LineVisualizationConfig.empty()) as LineVisualizationConfig;
  const { interpolation = 'linear', axisType = DEFAULT_AXIS_TYPE } = visualizationConfig;
  const mapKeys = useMapKeys();
  const rowPivotFields = useMemo(() => config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [], [config?.rowPivots]);
  const _mapKeys = useCallback((labels: string[]) => labels
    .map((label) => label.split(keySeparator)
      .map((l, i) => mapKeys(l, rowPivotFields[i]))
      .join(humanSeparator),
    ), [mapKeys, rowPivotFields]);
  const chartGenerator: Generator = useCallback(({ type, name, labels, values, originalName }) => ({
    type,
    name,
    x: _mapKeys(labels),
    y: values,
    originalName,
    line: { shape: toPlotly(interpolation) },
  }), [_mapKeys, interpolation]);

  const rows = useMemo(() => retrieveChartData(data), [data]);
  const _chartDataResult = useChartData(rows, {
    widgetConfig: config,
    chartType: 'scatter',
    generator: chartGenerator,
  });

  const { eventChartData, shapes } = useEvents(config, data.events);

  const chartDataResult = eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult;
  const layout = shapes ? { shapes } : {};

  return (
    <XYPlot config={config}
            plotLayout={layout}
            axisType={axisType}
            effectiveTimerange={effectiveTimerange}
            getChartColor={getChartColor}
            height={height}
            chartData={chartDataResult} />
  );
}, 'line');

LineVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: AggregationResult.isRequired,
  height: PropTypes.number,
};

export default LineVisualization;
