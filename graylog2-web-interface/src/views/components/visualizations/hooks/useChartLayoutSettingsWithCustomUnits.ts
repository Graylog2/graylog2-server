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
  const ticksConfig = useXAxisTicks(config, chartData);
  const unitFeatureEnabled = useFeature(UNIT_FEATURE_FLAG);
  const widgetUnits = useWidgetUnits(config);
  const { unitTypeMapper, fieldNameToAxisNameMapper } = useMemo(
    () => generateMappersForYAxis({ series: config.series, units: widgetUnits }),
    [config.series, widgetUnits],
  );
  const thresholdShapes = getThresholdShapes(config.series, widgetUnits, fieldNameToAxisNameMapper);

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

    const xA = 0.8 ?? '2025-09-09T02:25:30.000-07:00';
    const yA = 0.8 ?? 10;

    console.log({ chartData });
    const _layouts: Partial<Layout> = {
      ...generatedLayouts,
      annotations: [
        {
          x: xA,
          y: yA,
          text: 'Label',
          xref: 'paper',
          yref: 'paper',
          ax: 0,
          ay: -20,
          font: { size: 12, color: 'orange' },
          captureevents: true,
          showarrow: true,
          arrowhead: 4,
          arrowcolor: 'orange', // ← color of the square head + shaft
          hovertext: 'Some other text<br>Some text', // ← shows a popover on hover
          hoverlabel: { bgcolor: '#222', font: { color: '#fff' } },
        },
      ],
      shapes: [
        ...thresholdShapes,
        {
          type: 'line',
          x0: xA,
          x1: xA,
          y0: 0, // adjust to your actual axis min if not 0
          y1: yA,
          // xref: 'x',
          // yref: 'y',
          layer: 'above',
          line: { width: 2, dash: 'dot', color: 'orange' },
        },
        // horizontal dashed line to y-axis
        {
          type: 'line',
          x0: 0, // or use current x-axis min dynamically
          x1: xA,
          y0: yA,
          y1: yA,
          // xref: 'x',
          // yref: 'y',
          layer: 'above',
          line: { width: 2, dash: 'dot', color: 'orange' },
        },
      ],

      hovermode: 'x',
      xaxis: {
        domain: generateDomain(Object.keys(unitTypeMapper)?.length),
        ...ticksConfig,
      },
    };

    return _layouts;
  }, [
    barmode,
    chartData,
    config,
    theme,
    ticksConfig,
    thresholdShapes,
    unitFeatureEnabled,
    unitTypeMapper,
    widgetUnits,
  ]);
};

export default useChartLayoutSettingsWithCustomUnits;
