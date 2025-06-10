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

import zipWith from 'lodash/zipWith';
import sum from 'lodash/sum';
import flattenDeep from 'lodash/flattenDeep';
import type { DefaultTheme } from 'styled-components';

import type { FieldUnitType } from 'views/types';
import type Series from 'views/logic/aggregationbuilder/Series';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import type { BarMode } from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import {
  getBaseUnit,
  getPrettifiedValue,
  roundValue,
  convertValueToBaseUnit,
} from 'views/components/visualizations/utils/unitConverters';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import {
  DEFAULT_AXIS_KEY,
  TIME_AXIS_LABELS_QUANTITY,
  DECIMAL_PLACES,
  Y_POSITION_AXIS_STEP,
  NO_FIELD_NAME_SERIES,
} from 'views/components/visualizations/Constants';
import type UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';
import getFieldNameFromTrace from 'views/components/visualizations/utils/getFieldNameFromTrace';
import type { PieHoverTemplateSettings } from 'views/components/visualizations/hooks/usePieChartDataSettingsWithCustomUnits';
import getDefaultPlotYLayoutSettings from 'views/components/visualizations/utils/getDefaultPlotYLayoutSettings';
import formatValueWithUnitLabel from 'views/components/visualizations/utils/formatValueWithUnitLabel';

type DefaultAxisKey = 'withoutUnit';

const getYAxisPosition = (axisCount: number) => {
  const diff = Math.floor(axisCount / 2) * Y_POSITION_AXIS_STEP;

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

const getTicklabelPositionSettings = (axisCount: number) => {
  switch (axisCount) {
    case 4:
      return { ticklabelposition: 'inside' };
    default:
      return {};
  }
};

const getYAxisPositioningSettings = (axisCount: number) => ({
  position: getYAxisPosition(axisCount),
  side: getYAxisSide(axisCount),
  overlaying: axisCount > 1 ? 'y' : undefined,
  ...getTicklabelPositionSettings(axisCount),
});

const defaultSettings = {
  autoshift: true,
  automargin: true,
  fixedrange: true,
  rangemode: 'tozero',
};

const getFormatSettingsWithCustomTickVals = (values: Array<any>, fieldType: FieldUnitType) => {
  const min = Math.min(0, ...values);
  const max = Math.max(...values);
  const step = (max - min) / TIME_AXIS_LABELS_QUANTITY;

  const valueBaseUnit = getBaseUnit(fieldType);
  const prettyRoundedValues = Array(TIME_AXIS_LABELS_QUANTITY)
    .fill(null)
    .map((_, index) => {
      const originalBaseValue = (index + 1) * step;

      const prettyValue = getPrettifiedValue(originalBaseValue, {
        abbrev: valueBaseUnit.abbrev,
        unitType: valueBaseUnit.unitType,
      });

      return {
        value: roundValue(prettyValue.value),
        unit: prettyValue.unit,
      };
    });
  const roundedTickValues = prettyRoundedValues.map(
    (prettified) => convertValueToBaseUnit(prettified.value, prettified.unit).value,
  );
  const roundedTickText = prettyRoundedValues.map((prettified) =>
    formatValueWithUnitLabel(prettified?.value, prettified.unit.abbrev, 0),
  );

  return {
    tickvals: roundedTickValues,
    ticktext: roundedTickText,
    range: [0, roundedTickValues[TIME_AXIS_LABELS_QUANTITY - 1]],
  };
};

const getFormatSettingsByData = (unitTypeKey: FieldUnitType | DefaultAxisKey, values: Array<any>) => {
  switch (unitTypeKey) {
    case 'percent':
      return {
        tickformat: `.${DECIMAL_PLACES}%`,
      };
    case 'size':
      return getFormatSettingsWithCustomTickVals(values, 'size');
    case 'time':
      return getFormatSettingsWithCustomTickVals(values, 'time');
    default:
      return {
        tickformat: ',~r',
      };
  }
};

const getUnitLayoutWithData = (
  unitTypeKey: FieldUnitType | DefaultAxisKey,
  axisCount: number,
  values: Array<any>,
  theme: DefaultTheme,
) => ({
  ...getFormatSettingsByData(unitTypeKey, values),
  ...getYAxisPositioningSettings(axisCount),
  ...defaultSettings,
  ...getDefaultPlotYLayoutSettings(theme),
});

type SeriesName = string;
type AxisName = string;

/** Needs to group traces. In case if barmode: 'stack' | 'relative' | 'overlay'
 * we are grouping by y-axis to join traces into same trace on chart */
export const getBarChartTraceOffsetGroup = (
  barmode: BarMode,
  yaxis: string,
  traceIndex: number,
): string | number | undefined => {
  if (barmode === 'stack' || barmode === 'relative' || barmode === 'overlay') {
    return yaxis;
  }

  if (barmode === 'group') {
    return traceIndex;
  }

  return undefined;
};

export type UnitTypeMapper = {} | Record<FieldUnitType, { axisKeyName: string; axisCount: number }>;
type SeriesUnitMapper = {} | Record<SeriesName, FieldUnitType | DefaultAxisKey>;
type MapperAxisNumber = Record<string, number>;
type YAxisMapper = Record<SeriesName, AxisName>;
type FieldNameToAxisNameMapper = {} | Record<string, string>;
type FieldNameToAxisCountMapper = {} | Record<string, number>;
export type MappersForYAxis = {
  seriesUnitMapper: SeriesUnitMapper;
  mapperAxisNumber: MapperAxisNumber;
  unitTypeMapper: UnitTypeMapper;
  yAxisMapper: YAxisMapper;
  fieldNameToAxisNameMapper: FieldNameToAxisNameMapper;
  fieldNameToAxisCountMapper: FieldNameToAxisCountMapper;
};

export const generateMappersForYAxis = ({
  series,
  units,
}: {
  series: AggregationWidgetConfig['series'];
  units: AggregationWidgetConfig['units'];
}): MappersForYAxis => {
  let axisCount = 1;
  const unitTypeMapper: {} | UnitTypeMapper = {};
  const mapper = {};
  const mapperAxisNumber = {};
  const seriesUnitMapper = {};

  const fieldNameToAxisNameMapper = {};
  const fieldNameToAxisCountMapper = {};

  series.forEach((s: Series) => {
    const seriesName = s.config.name || s.function;
    const { field } = parseSeries(s.function) ?? {};
    const unitType = units?.getFieldUnit(field)?.unitType;
    const unitTypeKey = unitType || DEFAULT_AXIS_KEY;
    const fieldKey = field ?? NO_FIELD_NAME_SERIES;

    if (!unitTypeMapper[unitTypeKey]) {
      const axisNameNumberPart = axisCount > 1 ? axisCount : '';
      const axisKeyName = `yaxis${axisNameNumberPart}`;
      unitTypeMapper[unitTypeKey] = { axisCount, axisKeyName };

      mapper[seriesName] = `y${axisNameNumberPart}`;
      mapperAxisNumber[seriesName] = axisCount;

      fieldNameToAxisNameMapper[fieldKey] = `y${axisNameNumberPart}`;
      fieldNameToAxisCountMapper[fieldKey] = axisCount;

      axisCount += 1;
    } else {
      const currentAxisCount = unitTypeMapper[unitTypeKey].axisCount;
      const axisNameNumberPart = currentAxisCount > 1 ? currentAxisCount : '';
      mapper[seriesName] = `y${axisNameNumberPart}`;
      mapperAxisNumber[seriesName] = currentAxisCount;

      fieldNameToAxisNameMapper[fieldKey] = `y${axisNameNumberPart}`;
      fieldNameToAxisCountMapper[fieldKey] = currentAxisCount;
    }

    seriesUnitMapper[seriesName] = unitTypeKey;
  });

  return {
    unitTypeMapper,
    yAxisMapper: mapper,
    mapperAxisNumber,
    seriesUnitMapper,
    fieldNameToAxisNameMapper,
    fieldNameToAxisCountMapper,
  };
};

// eslint-disable-next-line default-param-last
const joinValues = (values: Array<Array<number>> = [], barmode: BarMode): Array<number> => {
  if (barmode === 'stack' || barmode === 'relative') {
    return zipWith(...values, (...iterateValues) => sum(iterateValues));
  }

  return flattenDeep(values);
};

export type GenerateLayoutsParams = {
  unitTypeMapper: UnitTypeMapper;
  chartData: Array<ChartDefinition>;
  barmode?: BarMode;
  widgetUnits: UnitsConfig;
  config: AggregationWidgetConfig;
  theme: DefaultTheme;
};

export const generateLayouts = ({
  unitTypeMapper,
  chartData,
  barmode,
  widgetUnits,
  config,
  theme,
}: GenerateLayoutsParams) => {
  const groupYValuesByUnitTypeKey = chartData.reduce<{} | Record<FieldUnitType | DefaultAxisKey, Array<Array<any>>>>(
    (res, value: ChartDefinition) => {
      const traceName = value.fullPath;
      const fieldName = getFieldNameFromTrace({ series: config.series, fullPath: traceName });
      const unit = widgetUnits.getFieldUnit(fieldName);
      const unitType = unit?.unitType ?? DEFAULT_AXIS_KEY;

      if (!res[unitType]) {
        res[unitType] = [value.y];
      } else {
        res[unitType].push(value.y);
      }

      return res;
    },
    {},
  );

  return Object.fromEntries(
    Object.entries(unitTypeMapper).map(([unitTypeKey, { axisKeyName, axisCount }]) => {
      const unitValues = joinValues(groupYValuesByUnitTypeKey[unitTypeKey], barmode);

      return [axisKeyName, getUnitLayoutWithData(unitTypeKey as FieldUnitType, axisCount, unitValues, theme)];
    }),
  );
};

const getHoverTexts = ({ convertedValues, unit }: { convertedValues: Array<any>; unit: FieldUnit }) =>
  convertedValues.map((value) => {
    const prettified =
      unit &&
      unit.isDefined &&
      getPrettifiedValue(value, {
        abbrev: unit.abbrev,
        unitType: unit.unitType,
      });

    if (!prettified) return value;

    return formatValueWithUnitLabel(prettified?.value, prettified.unit.abbrev);
  });

export const getHoverTemplateSettings = ({
  convertedValues,
  unit,
  name,
}: {
  convertedValues: Array<any>;
  unit: FieldUnit;
  name: string;
}): { text: Array<string>; hovertemplate: string; meta: string } | {} => {
  if (unit?.unitType === 'time' || unit?.unitType === 'size') {
    return {
      text: getHoverTexts({ convertedValues, unit }),
      hovertemplate: '%{text}<br><extra>%{meta}</extra>',
      meta: name,
    };
  }

  return {};
};

export const getPieHoverTemplateSettings = ({
  convertedValues,
  unit,
  name,
}: {
  convertedValues: Array<any>;
  unit: FieldUnit;
  name: string;
}): PieHoverTemplateSettings | {} => ({
  text: getHoverTexts({ convertedValues, unit }),
  hovertemplate: '<b>%{label}</b><br>%{text}<br>%{percent}',
  meta: name,
  textinfo: 'percent',
});
