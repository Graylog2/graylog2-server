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
import zipWith from 'lodash/zipWith';
import sum from 'lodash/sum';
import flattenDeep from 'lodash/flattenDeep';

import type { FieldUnitType } from 'views/types';
import type Series from 'views/logic/aggregationbuilder/Series';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import type { BarMode } from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import {
  getBaseUnit,
  getPrettifiedValue,
} from 'views/components/visualizations/utils/unitConvertors';
import { VALUE_WITH_UNIT_DIGITS } from 'views/components/TypeSpecificValue';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';

const Y_POSITION_AXIS_STEP = 0.1;
type DefaultAxisKey = 'withoutUnit';
const DEFAULT_AXIS_KEY = 'withoutUnit';
const TIME_AXIS_LABELS_QUANTITY = 4;

const getYAxisPosition = (axisCount: number) => {
  const diff = (Math.floor(axisCount / 2)) * Y_POSITION_AXIS_STEP;

  if (axisCount % 2 === 0) {
    return 1 - diff + Y_POSITION_AXIS_STEP;
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
  // ticklabelposition: 'outside left',
  /*
  standoff: 20,
  tickson: 'boundaries',
  tickangle: -45,

   */
});

const defaultSettings = {
  autoshift: true,
  automargin: true,
  fixedrange: true,
  rangemode: 'tozero',
};

const getFormatSettingsForTime = (values: Array<any>) => {
  const min = Math.min(0, ...values);
  const max = Math.max(...values);
  const step = (max - min) / TIME_AXIS_LABELS_QUANTITY;

  const tickvals = Array(TIME_AXIS_LABELS_QUANTITY).fill(null).map((_, index) => (index + 1) * step);
  const timeBaseUnit = getBaseUnit('time');
  const prettyValues = tickvals.map((value) => getPrettifiedValue(value, { abbrev: timeBaseUnit.abbrev, unitType: timeBaseUnit.unitType }));

  const ticktext = prettyValues.map((prettified) => `${Number(prettified?.value).toFixed(VALUE_WITH_UNIT_DIGITS)} ${prettified.unit.abbrev}`);

  return ({
    tickvals,
    ticktext,
  });
};

const getFormatSettingsByData = (unitTypeKey: FieldUnitType | DefaultAxisKey, values: Array<any>) => {
  switch (unitTypeKey) {
    case 'percent':
      return ({
        tickformat: '.1%',
      });
    case 'size':
      return ({
        tickformat: '.2s',
        ticksuffix: 'B',
      });
    case 'time':
      return getFormatSettingsForTime(values);
    default:
      return ({
        tickformat: ',~r',
      });
  }
};

export const getUnitLayoutWithData = (unitTypeKey: FieldUnitType | DefaultAxisKey, axisCount: number, values: Array<any>) => ({
  ...getFormatSettingsByData(unitTypeKey, values),
  ...getYAxisPositioningSettings(axisCount),
  ...defaultSettings,
});

type SeriesName = string;
type AxisName = string;

export const generateDomain = (yAxisCount: number) => {
  if (!yAxisCount || yAxisCount === 1) return [0, 1];
  const leftAxisCount = Math.ceil(yAxisCount / 2);
  const rightAxisCount = Math.floor(yAxisCount / 2);

  return [(leftAxisCount - 1) * Y_POSITION_AXIS_STEP, 1 - (rightAxisCount - 1) * Y_POSITION_AXIS_STEP];
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

type UnitTypeMapper = {} | Record<FieldUnitType, { axisKeyName: string, axisCount: number }>;
type SeriesUnitMapper = {} | Record<SeriesName, FieldUnitType | DefaultAxisKey>;
type MapperAxisNumber = Record<string, number>;
type YAxisMapper = Record<SeriesName, AxisName>;

export const generateMappersForYAxis = (
  { series, units }: { series: AggregationWidgetConfig['series'], units: AggregationWidgetConfig['units'] }): { seriesUnitMapper: SeriesUnitMapper, mapperAxisNumber: MapperAxisNumber, unitTypeMapper: UnitTypeMapper, yAxisMapper: YAxisMapper} => {
  let axisCount = 1;
  const unitTypeMapper: {} | UnitTypeMapper = {};
  const mapper = {};
  const mapperAxisNumber = {};
  const seriesUnitMapper = {};

  series.forEach((s: Series) => {
    const seriesName = s.config.name || s.function;
    const { field } = parseSeries(s.function) ?? {};
    const unitType = units?.getFieldUnit(field)?.unitType;
    const unitTypeKey = unitType || DEFAULT_AXIS_KEY;

    if (!unitTypeMapper[unitTypeKey]) {
      const axisNameNumberPart = axisCount > 1 ? axisCount : '';
      const axisKeyName = `yaxis${axisNameNumberPart}`;
      unitTypeMapper[unitTypeKey] = { axisCount, axisKeyName };

      mapper[seriesName] = `y${axisNameNumberPart}`;
      mapperAxisNumber[seriesName] = axisCount;
      axisCount += 1;
    } else {
      const currentAxisCount = unitTypeMapper[unitTypeKey].axisCount;
      const axisNameNumberPart = currentAxisCount > 1 ? currentAxisCount : '';
      mapper[seriesName] = `y${axisNameNumberPart}`;
      mapperAxisNumber[seriesName] = currentAxisCount;
    }

    seriesUnitMapper[seriesName] = unitTypeKey;
  });

  return ({
    unitTypeMapper,
    yAxisMapper: mapper,
    mapperAxisNumber,
    seriesUnitMapper,
  });
};

const joinValues = (values: Array<Array<number>>, barmode: BarMode): Array<number> => {
  if (barmode === 'stack' || barmode === 'relative') {
    return zipWith(...values, (...iterateValues) => sum(iterateValues));
  }

  return flattenDeep(values);
};

export const generateLayouts = (
  { unitTypeMapper, seriesUnitMapper, chartData, barmode }: { unitTypeMapper: UnitTypeMapper, seriesUnitMapper: SeriesUnitMapper, chartData: Array<ChartDefinition>, barmode?: string },
): Record<string, unknown> => {
  const groupYValuesByUnitTypeKey = chartData.reduce<{} | Record<FieldUnitType | DefaultAxisKey, Array<Array<any>>>>((res, value: ChartDefinition) => {
    const seriesName = value.name || value.originalName;
    const unitType = seriesUnitMapper[seriesName];

    if (!res[unitType]) {
      res[unitType] = [value.y];
    } else {
      res[unitType].push(value.y);
    }

    return res;
  }, {});

  return transform(unitTypeMapper, (res, { axisKeyName, axisCount }, unitTypeKey: FieldUnitType | DefaultAxisKey) => {
    const unitValues = joinValues(groupYValuesByUnitTypeKey[unitTypeKey], barmode);
    res[axisKeyName] = getUnitLayoutWithData(unitTypeKey, axisCount, unitValues);
  });
};

export const getHoverTemplateSettings = ({ convertedToBaseValues, curUnit, originalName }: {
  convertedToBaseValues: Array<any>,
  curUnit: FieldUnit,
  originalName: string,
}): { text: Array<string>, hovertemplate: string, meta: string } | {} => {
  if (curUnit?.unitType === 'time') {
    const timeBaseUnit = getBaseUnit('time');

    return ({
      text: convertedToBaseValues.map((value) => {
        const prettified = curUnit && curUnit.isDefined && getPrettifiedValue(value, {
          abbrev: timeBaseUnit.abbrev,
          unitType: timeBaseUnit.unitType,
        });

        if (!prettified) return null;

        return `${Number(prettified?.value).toFixed(1)} ${prettified.unit.abbrev}`;
      }),
      hovertemplate: '%{text}<br><extra>%{meta}</extra>',
      meta: originalName,
    });
  }

  return ({});
};
