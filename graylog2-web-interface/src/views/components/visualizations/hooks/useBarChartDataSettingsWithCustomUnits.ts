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
  getBarChartTraceOffsetSettings,
} from 'views/components/visualizations/utils/chartLayoutGenerators';
import type { BarMode } from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import { NO_FIELD_NAME_SERIES, UNIT_FEATURE_FLAG } from 'views/components/visualizations/Constants';
import getFieldNameFromTrace from 'views/components/visualizations/utils/getFieldNameFromTrace';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import useChartDataSettingsWithCustomUnits
  from 'views/components/visualizations/hooks/useChartDataSettingsWithCustomUnits';

const useBarChartDataSettingsWithCustomUnits = ({ config, barmode, effectiveTimerange }: {
  config: AggregationWidgetConfig,
  barmode?: BarMode,
  effectiveTimerange: AbsoluteTimeRange,
}) => {
  const unitFeatureEnabled = useFeature(UNIT_FEATURE_FLAG);
  const widgetUnits = useWidgetUnits(config);
  const { fieldNameToAxisCountMapper, unitTypeMapper } = useMemo(() => generateMappersForYAxis({ series: config.series, units: widgetUnits }), [config.series, widgetUnits]);
  const getChartDataSettingsWithCustomUnits = useChartDataSettingsWithCustomUnits({ config });

  return useCallback(({
    values, idx, total, xAxisItemsLength, fullPath, name,
  }: { xAxisItemsLength: number, originalName: string, name: string, values: Array<any>, idx: number, total: number, fullPath: string }):Partial<ChartDefinition> => {
    if (!unitFeatureEnabled) return ({});

    const fieldNameKey = getFieldNameFromTrace({ fullPath, series: config.series }) ?? NO_FIELD_NAME_SERIES;
    const { y: convertedValues, yaxis, ...hoverTemplateSettings } = getChartDataSettingsWithCustomUnits({ name, fullPath, values });
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
      fullPath,
      ...hoverTemplateSettings,
      ...offsetSettings,
    });
  }, [barmode, config.isTimeline, config.series, effectiveTimerange, fieldNameToAxisCountMapper, getChartDataSettingsWithCustomUnits, unitFeatureEnabled, unitTypeMapper]);
};

export default useBarChartDataSettingsWithCustomUnits;
