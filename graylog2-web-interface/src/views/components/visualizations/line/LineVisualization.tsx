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
import type { Layout } from 'plotly.js';

import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import toPlotly from 'views/logic/aggregationbuilder/visualizations/Interpolation';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';
import { DEFAULT_AXIS_TYPE } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import { keySeparator, humanSeparator } from 'views/Constants';
import useChartLayoutSettingsWithCustomUnits from 'views/components/visualizations/hooks/useChartLayoutSettingsWithCustomUnits';
import useChartDataSettingsWithCustomUnits from 'views/components/visualizations/hooks/useChartDataSettingsWithCustomUnits';

import XYPlot from '../XYPlot';
import type { Generator } from '../ChartData';

const LineVisualization = makeVisualization(({
  config,
  data,
  effectiveTimerange,
  height,
  width,
}: VisualizationComponentProps) => {
  const visualizationConfig = (config.visualizationConfig ?? LineVisualizationConfig.empty()) as LineVisualizationConfig;
  const getChartDataSettingsWithCustomUnits = useChartDataSettingsWithCustomUnits({ config });
  const { interpolation = 'linear', axisType = DEFAULT_AXIS_TYPE } = visualizationConfig;
  const mapKeys = useMapKeys();
  const rowPivotFields = useMemo(() => config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [], [config?.rowPivots]);
  const _mapKeys = useCallback((labels: string[]) => labels
    .map((label) => label.split(keySeparator)
      .map((l, i) => mapKeys(l, rowPivotFields[i]))
      .join(humanSeparator),
    ), [mapKeys, rowPivotFields]);

  const chartGenerator: Generator = useCallback(({ type, name, labels, values, originalName, fullPath }) => ({
    type,
    name,
    x: _mapKeys(labels),
    y: values,
    originalName,
    line: { shape: toPlotly(interpolation) },
    ...getChartDataSettingsWithCustomUnits({ name, fullPath, values }),
  }), [_mapKeys, getChartDataSettingsWithCustomUnits, interpolation]);

  const rows = useMemo(() => retrieveChartData(data), [data]);
  const _chartDataResult = useChartData(rows, {
    widgetConfig: config,
    chartType: 'scatter',
    generator: chartGenerator,
  });

  const { eventChartData, shapes } = useEvents(config, data.events);

  const chartDataResult = useMemo(() => (eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult), [_chartDataResult, eventChartData]);
  const getChartLayoutSettingsWithCustomUnits = useChartLayoutSettingsWithCustomUnits({ config, chartData: chartDataResult });
  const layout = useMemo<Partial<Layout>>(() => {
    const _layouts = shapes ? { shapes } : {};

    return ({ ..._layouts, ...getChartLayoutSettingsWithCustomUnits() });
  }, [shapes, getChartLayoutSettingsWithCustomUnits]);

  return (
    <XYPlot config={config}
            plotLayout={layout}
            axisType={axisType}
            effectiveTimerange={effectiveTimerange}
            height={height}
            width={width}
            chartData={chartDataResult} />
  );
}, 'line');

export default LineVisualization;
