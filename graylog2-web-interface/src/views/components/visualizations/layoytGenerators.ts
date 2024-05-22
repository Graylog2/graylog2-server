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

import transform from 'lodash/transform';

import type { MetricUnitType } from 'views/types';
import type Series from 'views/logic/aggregationbuilder/Series';
import type { BarMode } from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';

const Y_POSITION_AXIS_STEP = 0.08;
type DefaulAxisKey = 'withoutUnit';
const DEFAULT_AXIS_KEY = 'withoutUnit';

const getYAxisPosition = (axisCount: number) => {
  const diff = Math.floor(axisCount / 2) * Y_POSITION_AXIS_STEP;

  if (axisCount % 2 === 0) {
    return 1 - diff;
  }

  return diff;
};

const getYAxisSide = (axisCount: number) => {
  if (axisCount % 2 === 0) {
    return 'right';
  }

  return 'left';
};

const getYAxisPositioningSettings = (axisCount: number) => ({
  position: getYAxisPosition(axisCount),
  side: getYAxisSide(axisCount),
  overlaying: axisCount > 1 ? 'y' : undefined,
});

const defaultSettings = {
  autoshift: true,
  automargin: true,
  fixedrange: true,
  rangemode: 'tozero',
};

export const getFormatSettings = (unitTypeKey: MetricUnitType | DefaulAxisKey) => {
  switch (unitTypeKey) {
    case 'percent':
      return ({
        tickformat: '%',
      });
    case 'size':
      return ({
        tickformat: '.2s',
        ticksuffix: 'B',
      });
    case 'time':
      return ({
        type: 'date',
        // tickformat: '%S',
        tickformatstops: [
          { dtickrange: [0, 1000], value: '%Lms', name: 'milliseconds' }, //
          { dtickrange: [1000, 60000], value: '%Ss', name: 'seconds' }, //
          { dtickrange: [60000, 3600000], value: '%Mm', name: 'minutes' }, //
          { dtickrange: [3600000, Infinity], value: '%Hh', name: 'hours' },
        ],
      });
    default:
      return ({
        tickformat: ',~r',
      });
  }
};

const getTitleSettings = (unitType: string): { title: { text: string }} | {} => {
  if (!unitType || unitType === DEFAULT_AXIS_KEY) return {};

  return ({
    title: { text: unitType, automargin: true, yref: 'container' },
  });
};

export const getUnitLayout = (unitTypeKey: MetricUnitType | DefaulAxisKey, axisCount: number) => ({
  ...getFormatSettings(unitTypeKey),
  ...getYAxisPositioningSettings(axisCount),
  ...defaultSettings,
});

type SeriesName = string;
type AxisName = string;

export const generateYAxis = (series: Array<Series>): { mapperAxisNumber: Record<string, number>, layouts: Record<string, unknown>, yAxisMapper: Record<SeriesName, AxisName>} => {
  let axisCount = 1;
  const unitLayout: {} | Record<MetricUnitType, { layout: Record<string, unknown>, axisKeyName: string}> = {};
  const mapper = {};
  const mapperAxisNumber = {};
  const mapperAxisSeries = {};

  series.forEach((s: Series) => {
    const seriesName = s.config.name || s.function;
    const { unitType } = s.unit;
    const unitTypeKey = unitType || DEFAULT_AXIS_KEY;

    if (!unitLayout[unitTypeKey]) {
      const axisNameNumberPart = axisCount > 1 ? axisCount : '';
      const axisKeyName = `yaxis${axisNameNumberPart}`;
      unitLayout[unitTypeKey] = { layout: getUnitLayout(unitTypeKey, axisCount), axisCount, axisKeyName };

      mapper[seriesName] = `y${axisNameNumberPart}`;
      mapperAxisNumber[seriesName] = axisCount;
      mapperAxisSeries[axisKeyName] = [seriesName];
      axisCount += 1;
    } else {
      const currentAxisCount = unitLayout[unitTypeKey].axisCount;
      const axisNameNumberPart = currentAxisCount > 1 ? currentAxisCount : '';
      mapper[seriesName] = `y${axisNameNumberPart}`;
      mapperAxisNumber[seriesName] = currentAxisCount;
      mapperAxisSeries[`yaxis${axisNameNumberPart}`].push(seriesName);
    }
  });

  return ({
    layouts: transform(unitLayout, (res, { layout, axisKeyName }, key) => {
      res[axisKeyName] = { ...layout, ...getTitleSettings(key) };
    }),
    yAxisMapper: mapper,
    mapperAxisNumber,
  });
};

export const generateDomain = (yAxisCount: number) => {
  if (!yAxisCount || yAxisCount === 1) return [0, 1];
  const leftAxisCount = Math.ceil(yAxisCount / 2);
  const rightAxisCount = Math.floor(yAxisCount / 2);

  return [leftAxisCount * Y_POSITION_AXIS_STEP, 1 - rightAxisCount * Y_POSITION_AXIS_STEP];
};

const getWidth = (total: number) => (total <= 1 ? undefined : 1 / total);

const getOffset = (offsetNumber: number, totalOffsets: number) => {
  const width = getWidth(totalOffsets);
  if (!width) return undefined;
  const firstOffset = (width / 2) * (1 - totalOffsets);

  return firstOffset + width * (offsetNumber - 1);
};

type AdditionalSettings = {
  yaxis: string,
  totalAxis: number,
  axisNumber: number,
  traceIndex: number,
  totalTraces: number,
}

export const getBarChartTraceOffsetSettings = (barmode: BarMode, { yaxis, totalAxis, axisNumber, traceIndex, totalTraces }: AdditionalSettings) => {
  if (barmode === 'stack' || barmode === 'relative' || barmode === 'overlay') {
    const width = getWidth(totalAxis);
    const offset = getOffset(axisNumber, totalAxis);

    return ({
      offsetgroup: yaxis,
      width,
      offset,
    });
  }

  if (barmode === 'group') {
    const width = getWidth(totalTraces);
    const offset = getOffset(traceIndex + 1, totalTraces);

    return ({
      offsetgroup: traceIndex,
      width,
      offset,
    });
  }

  return ({});
};
