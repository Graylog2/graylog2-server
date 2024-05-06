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
import transform from 'lodash/transform';

import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { Shapes } from 'views/logic/searchtypes/events/EventHandler';
import { DateType } from 'views/logic/aggregationbuilder/Pivot';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import { keySeparator, humanSeparator } from 'views/Constants';
import type { ChartConfig } from 'views/components/visualizations/GenericPlot';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import type Series from 'views/logic/aggregationbuilder/Series';
import type { MetricUnitType } from 'views/types';

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

  console.log('defineSingleDateBarWidth defineSingleDateBarWidth defineSingleDateBarWidth defineSingleDateBarWidth', config.rowPivots.length !== 1 || config.rowPivots[0].type !== DateType);

  if (config.rowPivots.length !== 1 || config.rowPivots[0].type !== DateType) {
    console.log('WWWWIIIIIIDDDDTTTTHHHHHH', { chartDataResult });

    return chartDataResult;
  }

  return chartDataResult.map((data) => {
    console.log({
      LLL: data?.x?.length === 1,
      L: data?.x?.length,
      data,
    });

    if (data?.x?.length === 1) {
      // @ts-ignore
      const timeRangeMS = new Date(timeRangeTo) - new Date(timeRangeFrom);
      const widthXUnits = timeRangeMS * barWidth;

      console.log({
        fff: {
          ...data,
          width: [Math.max(minXUnits, widthXUnits)],
        },
      });

      return {
        ...data,
        width: [Math.max(minXUnits, widthXUnits)],
      };
    }

    return data;
  });
};

type Layout = {
  shapes?: Shapes;
  barmode?: string;
};

const AXIS_STEP = 0.05;

const getPosition = (axisCount: number) => {
  const diff = Math.floor(axisCount / 2) * AXIS_STEP;

  if (axisCount % 2 === 0) {
    return 1 - diff;
  }

  return diff;
};

const getSide = (axisCount: number) => {
  if (axisCount % 2 === 0) {
    return 'right';
  }

  return 'left';
};

const getUnitLayout = (unitType: MetricUnitType | 'withoutUnit', axisCount: number) => {
  switch (unitType) {
    case 'percent':
      return ({
        tickformat: '%',
        position: getPosition(axisCount),
        side: getSide(axisCount),
        autoshift: true,
        overlaying: axisCount > 1 ? 'y' : undefined,
        automargin: true,
      });
    case 'size':
      return ({
        tickformat: 's',
        hoverformat: '.4s',
        ticksuffix: 'bytes',
        position: getPosition(axisCount),
        side: getSide(axisCount),
        autoshift: true,
        overlaying: axisCount > 1 ? 'y' : undefined,
        automargin: true,
      });
    case 'time':
      return ({
        tickformatstops: [
          { dtickrange: [0, 1000], value: '%S ms' }, // Milliseconds
          { dtickrange: [1000, 60000], value: '%M Min %S sec' }, // Seconds and minutes
          { dtickrange: [60000, 3600000], value: '%H H %M Min' }, // Minutes and hours
          { dtickrange: [3600000, Number.MAX_SAFE_INTEGER], value: '%d days' }, // Hours and days
          // Add more custom tick formats as needed
        ],
        position: getPosition(axisCount),
        side: getSide(axisCount),
        autoshift: true,
        overlaying: axisCount > 1 ? 'y' : undefined,
        automargin: true,
      });
    default:
      return ({
        position: getPosition(axisCount),
        side: getSide(axisCount),
        fixedrange: true,
        rangemode: 'tozero',
        tickformat: ',~r',
        autoshift: true,
        overlaying: axisCount > 1 ? 'y' : undefined,
        automargin: true,
      });
  }
};

const generateYAxis2 = (series: Array<Series>): { layouts: Record<string, unknown>, yAxisMapper: Record<string, string>} => {
  let axisCount = 0;
  const unitLayout: {} | Record<MetricUnitType, { layout: Record<string, unknown>, axisKeyName: string}> = {};
  const mapper = {};

  series.forEach((s: Series) => {
    const seriesName = s.config.name || s.function;
    const { unitType } = s.unit;
    const unitTypeKey = unitType ?? 'withoutUnit';

    if (!unitLayout[unitTypeKey]) {
      axisCount += 1;
      const axisNameNumberPart = axisCount > 1 ? axisCount : '';
      unitLayout[unitTypeKey] = { layout: getUnitLayout(unitTypeKey, axisCount), axisKeyName: `yaxis${axisNameNumberPart}` };

      console.log({ axisCount, unitTypeKey });
      mapper[seriesName] = `y${axisNameNumberPart}`;
    } else {
      const axisNameNumberPart = axisCount > 1 ? axisCount : '';
      mapper[seriesName] = `y${axisNameNumberPart}`;
    }
  });

  return ({
    layouts: transform(unitLayout, (res, { layout, axisKeyName }) => {
      res[axisKeyName] = layout;
    }),
    yAxisMapper: mapper,
  });
};

const generateYAxis = (series: Array<Series>): { layouts: Record<string, unknown>, yAxisMapper: Record<string, string>} => {
  let axisCount = 1;
  const unitLayout: {} | Record<MetricUnitType, { layout: Record<string, unknown>, axisKeyName: string}> = {};
  const mapper = {};

  series.forEach((s: Series) => {
    const seriesName = s.config.name || s.function;
    const { unitType } = s.unit;
    const unitTypeKey = unitType ?? 'withoutUnit';

    if (!unitLayout[unitTypeKey]) {
      const axisNameNumberPart = axisCount > 1 ? axisCount : '';
      unitLayout[unitTypeKey] = { layout: getUnitLayout(unitTypeKey, axisCount), axisCount, axisKeyName: `yaxis${axisNameNumberPart}` };

      console.log({ axisCount, unitTypeKey });
      mapper[seriesName] = `y${axisNameNumberPart}`;
      axisCount += 1;
    } else {
      const currentAxisCount = unitLayout[unitTypeKey].axisCount;
      const axisNameNumberPart = currentAxisCount > 1 ? currentAxisCount : '';
      mapper[seriesName] = `y${axisNameNumberPart}`;
    }
  });

  return ({
    layouts: transform(unitLayout, (res, { layout, axisKeyName }) => {
      res[axisKeyName] = layout;
    }),
    yAxisMapper: mapper,
  });
};

const generateDomain = (yAxisCount: number) => {
  if (!yAxisCount || yAxisCount === 1) return [0, 1];
  const leftAxisCount = Math.ceil(yAxisCount / 2);
  const rightAxisCount = Math.floor(yAxisCount / 2);

  return [leftAxisCount * AXIS_STEP, 1 - rightAxisCount * AXIS_STEP];
};

const BarVisualization = makeVisualization(({
  config,
  data,
  effectiveTimerange,
  height,
}: VisualizationComponentProps) => {
  const visualizationConfig = (config.visualizationConfig ?? BarVisualizationConfig.empty()) as BarVisualizationConfig;
  const { layouts, yAxisMapper } = useMemo(() => generateYAxis(config.series), [config.series]);
  const _layout = useMemo(() => ({
    ...layouts,
    hovermode: 'x',
    xaxis: { domain: generateDomain(Object.keys(layouts)?.length) },
    barmode: visualizationConfig && visualizationConfig.barmode ? visualizationConfig.barmode : undefined,
  }), [layouts, visualizationConfig]);

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

    console.log({ total });
    const chart44 = () => ({
      type,
      name,
      x: _mapKeys(labels),
      y: values,
      opacity,
      yaxis,
      originalName,
      // width: 1 / total,
      // offsetgroup: idx,
      // offset: idx,
    });

    console.log({ yaxis, 'yAxisMapper[name]': yAxisMapper[name], originalName, name, yAxisMapper, chart44: chart44() });

    return chart44();
  },
  [_mapKeys, visualizationConfig?.opacity, yAxisMapper]);

  const rows = useMemo(() => retrieveChartData(data), [data]);
  const _chartDataResult = useChartData(rows, { widgetConfig: config, chartType: 'bar', generator: _seriesGenerator });

  const { eventChartData, shapes } = useEvents(config, data.events);

  const layout = shapes ? { ..._layout, shapes } : _layout;

  const chartData = useMemo(() => {
    const chartDataResult = eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult;

    console.log('INSIDE chartData', { chartDataResult, _chartDataResult, eventChartData });

    return defineSingleDateBarWidth(chartDataResult, config, effectiveTimerange?.from, effectiveTimerange?.to);
  }, [_chartDataResult, config, effectiveTimerange?.from, effectiveTimerange?.to, eventChartData]);

  console.log('DDDDDDDDDDD', { layout, chartData, _chartDataResult, eventChartData });

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
