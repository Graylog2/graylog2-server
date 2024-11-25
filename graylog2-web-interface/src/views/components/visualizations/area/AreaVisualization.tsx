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

import toPlotly from 'views/logic/aggregationbuilder/visualizations/Interpolation';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';
import { keySeparator, humanSeparator } from 'views/Constants';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import useChartDataSettingsWithCustomUnits from 'views/components/visualizations/hooks/useChartDataSettingsWithCustomUnits';
import useChartLayoutSettingsWithCustomUnits from 'views/components/visualizations/hooks/useChartLayoutSettingsWithCustomUnits';

import XYPlot from '../XYPlot';
import type { Generator } from '../ChartData';

const AreaVisualization = makeVisualization(({
  config,
  data,
  effectiveTimerange,
  height,
  width,
}: VisualizationComponentProps) => {
  const visualizationConfig = (config.visualizationConfig || AreaVisualizationConfig.empty()) as AreaVisualizationConfig;
  const getChartDataSettingsWithCustomUnits = useChartDataSettingsWithCustomUnits({ config });
  const { interpolation = 'linear' } = visualizationConfig;
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
    fill: 'tozeroy',
    line: { shape: toPlotly(interpolation) },
    originalName,
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
            axisType={visualizationConfig.axisType}
            plotLayout={layout}
            effectiveTimerange={effectiveTimerange}
            height={height}
            width={width}
            chartData={chartDataResult} />
  );
}, 'area');

export default AreaVisualization;
