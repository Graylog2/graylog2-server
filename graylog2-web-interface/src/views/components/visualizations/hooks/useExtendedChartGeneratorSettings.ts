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
import { useCallback, useMemo } from 'react';

import useFeature from 'hooks/useFeature';
import useWidgetUnits from 'views/components/visualizations/hooks/useWidgetUnits';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import {
  generateMappersForYAxis,
  getBarChartTraceOffsetSettings, getHoverTemplateSettings,
} from 'views/components/visualizations/utils/chartLayoytGenerators';
import type { BarMode } from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import convertDataToBaseUnit from 'views/components/visualizations/utils/convertDataToBaseUnit';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import { NO_FIELD_NAME_SERIES } from 'views/components/visualizations/Constants';
import { getBaseUnit } from 'views/components/visualizations/utils/unitConvertors';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import isLayoutRequiresBaseUnit from 'views/components/visualizations/utils/isLayoutRequiresBaseUnit';

const useExtendedChartGeneratorSettings = ({ config, barmode, effectiveTimerange }: {
  config: AggregationWidgetConfig,
  barmode?: BarMode,
  effectiveTimerange: AbsoluteTimeRange,
}) => {
  const unitFeatureEnabled = useFeature('configuration_of_formatting_value');
  const widgetUnits = useWidgetUnits(config);
  const { fieldNameToAxisNameMapper, fieldNameToAxisCountMapper, unitTypeMapper } = useMemo(() => generateMappersForYAxis({ series: config.series, units: widgetUnits }), [config.series, widgetUnits]);
  const getExtendedChartGeneratorSettings = useCallback(({ originalName, name, values }: { originalName: string, name: string, values: Array<any> }) => {
    if (!unitFeatureEnabled) return ({});

    const fieldNameKey = parseSeries(name)?.field ?? NO_FIELD_NAME_SERIES;
    const yaxis = fieldNameToAxisNameMapper[fieldNameKey];
    const curUnit = widgetUnits.getFieldUnit(fieldNameKey);
    const shouldConvertToBaseUnit = isLayoutRequiresBaseUnit(curUnit);
    const convertedValues = shouldConvertToBaseUnit ? convertDataToBaseUnit(values, curUnit) : values;
    const baseUnit = shouldConvertToBaseUnit && getBaseUnit(curUnit.unitType);
    const unit = shouldConvertToBaseUnit ? new FieldUnit(baseUnit.unitType, baseUnit.abbrev) : curUnit;

    return ({
      yaxis,
      y: convertedValues,
      ...getHoverTemplateSettings({ unit, convertedValues, originalName }),
    });
  }, [fieldNameToAxisNameMapper, unitFeatureEnabled, widgetUnits]);
  const getExtendedBarsGeneratorSettings = useCallback(({
    originalName, name, values, idx, total, xAxisItemsLength,
  }: { xAxisItemsLength: number, originalName: string, name: string, values: Array<any>, idx: number, total: number }) => {
    if (!unitFeatureEnabled) return ({});

    const fieldNameKey = parseSeries(name)?.field ?? NO_FIELD_NAME_SERIES;
    const { y: convertedValues, yaxis, ...hoverTemplateSettings } = getExtendedChartGeneratorSettings({ originalName, name, values });
    const axisNumber = fieldNameToAxisCountMapper?.[fieldNameKey];
    const totalAxis = Object.keys(unitTypeMapper).length;

    const offsetSettings = getBarChartTraceOffsetSettings(barmode, {
      yaxis,
      totalAxis,
      axisNumber,
      traceIndex: idx,
      totalTraces: total,
      effectiveTimerange,
      isTimeline: config.isTimeline,
      xAxisItemsLength: xAxisItemsLength,
    });

    return ({
      yaxis,
      y: convertedValues,
      ...hoverTemplateSettings,
      ...offsetSettings,
    });
  }, [barmode, config.isTimeline, effectiveTimerange, fieldNameToAxisCountMapper, getExtendedChartGeneratorSettings, unitFeatureEnabled, unitTypeMapper]);

  return ({
    getExtendedBarsGeneratorSettings,
    getExtendedChartGeneratorSettings,
  });
};

export default useExtendedChartGeneratorSettings;
