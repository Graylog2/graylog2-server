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

const getPositioningSettings = (axisCount: number) => ({
  position: getPosition(axisCount),
  side: getSide(axisCount),
  overlaying: axisCount > 1 ? 'y' : undefined,
});

const defaultSettings = {
  autoshift: true,
  automargin: true,
  fixedrange: true,
  rangemode: 'tozero',
};

export const getFormatSettings = (unitTypeKey: MetricUnitType | 'withoutUnit') => {
  switch (unitTypeKey) {
    case 'percent':
      return ({
        tickformat: '%',
      });
    case 'size':
      return ({
        tickformat: 's',
        hoverformat: '.4s',
        ticksuffix: 'bytes',
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
      });
    default:
      return ({
        tickformat: ',~r',
      });
  }
};

export const getUnitLayout = (unitTypeKey: MetricUnitType | 'withoutUnit', axisCount: number) => ({
  ...getFormatSettings(unitTypeKey),
  ...getPositioningSettings(axisCount),
  ...defaultSettings,
});

export const generateYAxis = (series: Array<Series>): { mapperAxisNumber: Record<string, number>, layouts: Record<string, unknown>, yAxisMapper: Record<string, string>} => {
  let axisCount = 1;
  const unitLayout: {} | Record<MetricUnitType, { layout: Record<string, unknown>, axisKeyName: string}> = {};
  const mapper = {};
  const mapperAxisNumber = {};

  series.forEach((s: Series) => {
    const seriesName = s.config.name || s.function;
    const { unitType } = s.unit;
    const unitTypeKey = unitType || 'withoutUnit';

    if (!unitLayout[unitTypeKey]) {
      const axisNameNumberPart = axisCount > 1 ? axisCount : '';
      unitLayout[unitTypeKey] = { layout: getUnitLayout(unitTypeKey, axisCount), axisCount, axisKeyName: `yaxis${axisNameNumberPart}` };

      mapper[seriesName] = `y${axisNameNumberPart}`;
      mapperAxisNumber[seriesName] = axisCount;
      axisCount += 1;
    } else {
      const currentAxisCount = unitLayout[unitTypeKey].axisCount;
      const axisNameNumberPart = currentAxisCount > 1 ? currentAxisCount : '';
      mapper[seriesName] = `y${axisNameNumberPart}`;
      mapperAxisNumber[seriesName] = currentAxisCount;
    }
  });

  return ({
    layouts: transform(unitLayout, (res, { layout, axisKeyName }) => {
      res[axisKeyName] = layout;
    }),
    yAxisMapper: mapper,
    mapperAxisNumber,
  });
};

export const generateDomain = (yAxisCount: number) => {
  if (!yAxisCount || yAxisCount === 1) return [0, 1];
  const leftAxisCount = Math.ceil(yAxisCount / 2);
  const rightAxisCount = Math.floor(yAxisCount / 2);

  return [leftAxisCount * AXIS_STEP, 1 - rightAxisCount * AXIS_STEP];
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

export const getTraceOffsetSettings = (barmode: BarMode, { yaxis, totalAxis, axisNumber, traceIndex, totalTraces }: AdditionalSettings) => {
  if (barmode === 'stack' || barmode === 'relative') {
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
