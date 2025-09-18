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
import type { Layout } from 'plotly.js';
import { useTheme } from 'styled-components';

import { generateLayouts, generateMappersForYAxis } from 'views/components/visualizations/utils/chartLayoutGenerators';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { BarMode } from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import useWidgetUnits from 'views/components/visualizations/hooks/useWidgetUnits';
import useFeature from 'hooks/useFeature';
import { UNIT_FEATURE_FLAG } from 'views/components/visualizations/Constants';
import generateDomain from 'views/components/visualizations/utils/generateDomain';
import useXAxisTicks from 'views/components/visualizations/hooks/useXAxisTicks';
import getThresholdShapes from 'views/components/visualizations/utils/getThresholdShapes';
import getWidgetAnnotations from 'views/components/visualizations/utils/getWidgetAnnotations';
import useUserDateTime from 'hooks/useUserDateTime';

const useChartLayoutSettingsWithCustomUnits = ({
  config,
  barmode,
  chartData,
}: {
  config: AggregationWidgetConfig;
  barmode?: BarMode;
  chartData: Array<ChartDefinition>;
}) => {
  const theme = useTheme();
  const { formatTime } = useUserDateTime();

  const ticksConfig = useXAxisTicks(config, chartData);
  const unitFeatureEnabled = useFeature(UNIT_FEATURE_FLAG);
  const widgetUnits = useWidgetUnits(config);
  const { unitTypeMapper, fieldNameToAxisNameMapper } = useMemo(
    () => generateMappersForYAxis({ series: config.series, units: widgetUnits }),
    [config.series, widgetUnits],
  );
  const thresholdShapes = getThresholdShapes(config.series, widgetUnits, fieldNameToAxisNameMapper);
  const { widgetAnnotations, referenceLineShapes } = getWidgetAnnotations(
    config,
    fieldNameToAxisNameMapper,
    theme,
    chartData,
    formatTime,
  );

  return useCallback(() => {
    if (!unitFeatureEnabled)
      return {
        xaxis: {
          ...ticksConfig,
        },
      };

    const generatedLayouts = generateLayouts({
      unitTypeMapper,
      barmode,
      chartData,
      widgetUnits,
      config,
      theme,
    });

    console.log({
      referenceLineShapes,
      widgetAnnotations,
    });
    const _layouts: Partial<Layout> = {
      ...generatedLayouts,
      shapes: [...thresholdShapes, ...referenceLineShapes],
      annotations: widgetAnnotations,
      hovermode: 'x',
      xaxis: {
        domain: generateDomain(Object.keys(unitTypeMapper)?.length),
        ...ticksConfig,
      },
    };

    console.log({ _layouts });

    return _layouts;
  }, [
    unitFeatureEnabled,
    ticksConfig,
    unitTypeMapper,
    barmode,
    chartData,
    widgetUnits,
    config,
    theme,
    referenceLineShapes,
    widgetAnnotations,
    thresholdShapes,
  ]);
};

export default useChartLayoutSettingsWithCustomUnits;
