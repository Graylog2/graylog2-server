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
import getSeriesUnit from 'views/components/visualizations/utils/getSeriesUnit';
import convertDataToBaseUnit from 'views/components/visualizations/utils/convertDataToBaseUnit';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';

const useExtendedChartGeneratorSettings = ({ config, barmode, effectiveTimerange }: {
  config: AggregationWidgetConfig,
  barmode?: BarMode,
  effectiveTimerange: AbsoluteTimeRange,
}) => {
  const unitFeatureEnabled = useFeature('configuration_of_formatting_value');
  const widgetUnits = useWidgetUnits(config);
  const { yAxisMapper, mapperAxisNumber, unitTypeMapper } = useMemo(() => generateMappersForYAxis({ series: config.series, units: widgetUnits }), [config.series, widgetUnits]);
  const getExtendedChartGeneratorSettings = useCallback(({ originalName, name, values }: { originalName: string, name: string, values: Array<any> }) => {
    if (!unitFeatureEnabled) return ({});

    const yaxis = yAxisMapper[name];
    const curUnit = getSeriesUnit(config.series, name || originalName, widgetUnits);
    const convertedToBaseUnitValues = convertDataToBaseUnit(values, curUnit);

    return ({
      yaxis,
      y: convertedToBaseUnitValues,
      ...getHoverTemplateSettings({ curUnit, convertedToBaseValues: convertedToBaseUnitValues, originalName }),
    });
  }, [config.series, unitFeatureEnabled, widgetUnits, yAxisMapper]);
  const getExtendedBarsGeneratorSettings = useCallback(({
    originalName, name, values, idx, total, xAxisItemsLength,
  }: { xAxisItemsLength: number, originalName: string, name: string, values: Array<any>, idx: number, total: number }) => {
    if (!unitFeatureEnabled) return ({});

    const { y: convertedToBaseUnitValues, yaxis, ...hoverTemplateSettings } = getExtendedChartGeneratorSettings({ originalName, name, values });
    const axisNumber = mapperAxisNumber?.[name];
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
      y: convertedToBaseUnitValues,
      ...hoverTemplateSettings,
      ...offsetSettings,
    });
  }, [barmode, config.isTimeline, effectiveTimerange, getExtendedChartGeneratorSettings, mapperAxisNumber, unitFeatureEnabled, unitTypeMapper]);

  return ({
    getExtendedBarsGeneratorSettings,
    getExtendedChartGeneratorSettings,
  });
};

export default useExtendedChartGeneratorSettings;
