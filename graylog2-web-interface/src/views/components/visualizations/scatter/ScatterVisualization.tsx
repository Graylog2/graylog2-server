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
import { useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';

import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';
import ScatterVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/ScatterVisualizationConfig';
import type { Generator } from 'views/components/visualizations/ChartData';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import { keySeparator, humanSeparator } from 'views/Constants';
import { generateDomain, generateYAxis } from 'views/components/visualizations/layoytGenerators';

import XYPlot from '../XYPlot';

const ScatterVisualization = makeVisualization(({
  config,
  data,
  effectiveTimerange,
  height,
}: VisualizationComponentProps) => {
  const visualizationConfig = (config.visualizationConfig ?? ScatterVisualizationConfig.empty()) as ScatterVisualizationConfig;
  const { layouts, yAxisMapper } = useMemo(() => generateYAxis(config.series), [config.series]);
  const _layout = useMemo(() => ({
    ...layouts,
    hovermode: 'x',
    xaxis: { domain: generateDomain(Object.keys(layouts)?.length) },
  }), [layouts]);
  const mapKeys = useMapKeys();
  const rowPivotFields = useMemo(() => config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [], [config?.rowPivots]);
  const _mapKeys = useCallback((labels: string[]) => labels
    .map((label) => label.split(keySeparator)
      .map((l, i) => mapKeys(l, rowPivotFields[i]))
      .join(humanSeparator),
    ), [mapKeys, rowPivotFields]);
  const rows = useMemo(() => retrieveChartData(data), [data]);
  const seriesGenerator: Generator = useCallback(({ type, name, labels, values, originalName }) => {
    const yaxis = yAxisMapper[name];

    return ({
      type,
      name,
      x: _mapKeys(labels),
      y: values,
      mode: 'markers',
      originalName,
      yaxis,
    });
  }, [_mapKeys, yAxisMapper]);
  const _chartDataResult = useChartData(rows, {
    widgetConfig: config,
    chartType: 'scatter',
    generator: seriesGenerator,
  });
  const { eventChartData, shapes } = useEvents(config, data.events);
  const chartDataResult = eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult;
  const layout = shapes ? { ..._layout, shapes } : _layout;

  return (
    <XYPlot config={config}
            axisType={visualizationConfig.axisType}
            chartData={chartDataResult}
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
