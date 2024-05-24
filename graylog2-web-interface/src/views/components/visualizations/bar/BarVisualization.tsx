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
import { DateType } from 'views/logic/aggregationbuilder/Pivot';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import { keySeparator, humanSeparator } from 'views/Constants';
import type { ChartConfig } from 'views/components/visualizations/GenericPlot';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import {
  generateDomain,
  generateYAxis,
  getBarChartTraceOffsetSettings,
} from 'views/components/visualizations/utils/chartLayoytGenerators';
import getSeriesUnit from 'views/components/visualizations/utils/getSeriesUnit';
import useFieldUnitTypes from 'hooks/useFieldUnitTypes';
import dataConvertor from 'views/components/visualizations/utils/dataConvertor';

import type { Generator } from '../ChartData';
import XYPlot from '../XYPlot';

type ChartDefinition = {
  type: string,
  name: string,
  x?: Array<string>,
  y?: Array<any>,
  z?: Array<Array<any>>,
  opacity?: number,
  originalName: string,
  unit?: string,
  yaxis?: string,
};

const setChartColor = (chart: ChartConfig, colors: ColorMapper) => ({ marker: { color: colors.get(chart.originalName ?? chart.name) } });

const defineSingleDateBarWidth = (chartDataResult: ChartDefinition[], config: AggregationWidgetConfig, timeRangeFrom: string, timeRangeTo: string) => {
  const barWidth = 0.03; // width in percentage, relative to chart width
  const minXUnits = 30;

  if (config.rowPivots.length !== 1 || config.rowPivots[0].type !== DateType) {
    return chartDataResult;
  }

  return chartDataResult.map((data) => {
    if (data?.x?.length === 1) {
      // @ts-ignore
      const timeRangeMS = new Date(timeRangeTo) - new Date(timeRangeFrom);
      const widthXUnits = timeRangeMS * barWidth;

      return {
        ...data,
        width: [Math.max(minXUnits, widthXUnits)],
      };
    }

    return data;
  });
};

const BarVisualization = makeVisualization(({
  config,
  data,
  effectiveTimerange,
  height,
}: VisualizationComponentProps) => {
  const { convertValueToUnit } = useFieldUnitTypes();
  const visualizationConfig = (config.visualizationConfig ?? BarVisualizationConfig.empty()) as BarVisualizationConfig;
  const { layouts, yAxisMapper, mapperAxisNumber } = useMemo(() => generateYAxis(config.series), [config.series]);
  const barmode = useMemo(() => (visualizationConfig && visualizationConfig.barmode ? visualizationConfig.barmode : undefined), [visualizationConfig]);
  const _layout = useMemo(() => ({
    ...layouts,
    hovermode: 'x',
    xaxis: { domain: generateDomain(Object.keys(layouts)?.length) },
    barmode,
  }), [barmode, layouts]);

  const mapKeys = useMapKeys();
  const rowPivotFields = useMemo(() => config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [], [config?.rowPivots]);
  const _mapKeys = useCallback((labels: string[]) => labels
    .map((label) => label.split(keySeparator)
      .map((l, i) => mapKeys(l, rowPivotFields[i]))
      .join(humanSeparator),
    ), [mapKeys, rowPivotFields]);

  const _seriesGenerator: Generator = useCallback(({ type, name, labels, values, originalName, total, idx }): ChartDefinition => {
    const yaxis = yAxisMapper[name];
    const opacity = visualizationConfig?.opacity ?? 1.0;
    const axisNumber = mapperAxisNumber?.[name];
    const totalAxis = Object.keys(layouts).length;

    const offsetSettings = getBarChartTraceOffsetSettings(barmode, { yaxis, totalAxis, axisNumber, traceIndex: idx, totalTraces: total });
    const curUnit = getSeriesUnit(config.series, name || originalName);

    const getData = () => ({
      type,
      name,
      x: _mapKeys(labels),
      y: dataConvertor(values, convertValueToUnit, curUnit),
      opacity,
      yaxis,
      originalName,
      ...offsetSettings,
    });

    return getData();
  },
  [convertValueToUnit, _mapKeys, barmode, layouts, mapperAxisNumber, visualizationConfig?.opacity, yAxisMapper]);

  const rows = useMemo(() => retrieveChartData(data), [data]);
  const _chartDataResult = useChartData(rows, { widgetConfig: config, chartType: 'bar', generator: _seriesGenerator });

  const { eventChartData, shapes } = useEvents(config, data.events);

  const layout = shapes ? { ..._layout, shapes } : _layout;

  const chartData = useMemo(() => {
    const chartDataResult = eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult;

    return defineSingleDateBarWidth(chartDataResult, config, effectiveTimerange?.from, effectiveTimerange?.to);
  }, [_chartDataResult, config, effectiveTimerange?.from, effectiveTimerange?.to, eventChartData]);

  return (
    <XYPlot config={config}
            axisType={visualizationConfig.axisType}
            chartData={chartData}
            effectiveTimerange={effectiveTimerange}
            setChartColor={setChartColor}
            height={height}
            plotLayout={layout} />
  );
}, 'bar');

BarVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: AggregationResult.isRequired,
  height: PropTypes.number,
};

export default BarVisualization;
