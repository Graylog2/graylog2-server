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
  generateDomain, generateLayouts, generateMappersForYAxis,
  getBarChartTraceOffsetSettings, getHoverTemplateSettings,
} from 'views/components/visualizations/utils/chartLayoytGenerators';
import getSeriesUnit from 'views/components/visualizations/utils/getSeriesUnit';
import convertDataToBaseUnit from 'views/components/visualizations/utils/convertDataToBaseUnit';
import useWidgetUnits from 'views/components/visualizations/hooks/useWidgetUnits';

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
  const widgetUnits = useWidgetUnits(config);
  const visualizationConfig = (config.visualizationConfig ?? BarVisualizationConfig.empty()) as BarVisualizationConfig;

  const { seriesUnitMapper, yAxisMapper, mapperAxisNumber, unitTypeMapper } = useMemo(() => generateMappersForYAxis({ series: config.series, units: widgetUnits }), [config.series, widgetUnits]);
  const barmode = useMemo(() => (visualizationConfig && visualizationConfig.barmode ? visualizationConfig.barmode : undefined), [visualizationConfig]);

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
    const totalAxis = Object.keys(unitTypeMapper).length;

    const offsetSettings = getBarChartTraceOffsetSettings(barmode, { yaxis, totalAxis, axisNumber, traceIndex: idx, totalTraces: total });
    const curUnit = getSeriesUnit(config.series, name || originalName, widgetUnits);

    const convertedToBaseUnitValues = convertDataToBaseUnit(values, curUnit);

    const getData = () => ({
      type,
      name,
      x: _mapKeys(labels),
      y: convertedToBaseUnitValues,
      opacity,
      yaxis,
      originalName,
      ...getHoverTemplateSettings({ curUnit, convertedToBaseValues: convertedToBaseUnitValues, originalName }),
      ...offsetSettings,
    });

    return getData();
  },
  [yAxisMapper, visualizationConfig?.opacity, mapperAxisNumber, unitTypeMapper, barmode, config.series, widgetUnits, _mapKeys]);

  const rows = useMemo(() => retrieveChartData(data), [data]);

  const _chartDataResult = useChartData(rows, { widgetConfig: config, chartType: 'bar', generator: _seriesGenerator });

  const { eventChartData, shapes } = useEvents(config, data.events);

  // const layout = shapes ? { ..._layout, shapes } : _layout;

  const chartData = useMemo(() => {
    const chartDataResult = eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult;

    return defineSingleDateBarWidth(chartDataResult, config, effectiveTimerange?.from, effectiveTimerange?.to);
  }, [_chartDataResult, config, effectiveTimerange?.from, effectiveTimerange?.to, eventChartData]);

  const layout = useMemo(() => {
    const generatedLayouts = generateLayouts({ unitTypeMapper, seriesUnitMapper, barmode, chartData });
    const _layouts = ({
      ...generatedLayouts,
      hovermode: 'x',
      xaxis: { domain: generateDomain(Object.keys(unitTypeMapper)?.length) },
      barmode,
    });

    if (shapes) {
      _layouts.shapes = shapes;
    }

    return _layouts;
  }, [unitTypeMapper, seriesUnitMapper, barmode, chartData, shapes]);

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
