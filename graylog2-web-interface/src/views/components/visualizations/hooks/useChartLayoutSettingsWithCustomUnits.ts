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

import {
  generateLayouts,
  generateMappersForYAxis,
} from 'views/components/visualizations/utils/chartLayoutGenerators';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { BarMode } from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import useWidgetUnits from 'views/components/visualizations/hooks/useWidgetUnits';
import useFeature from 'hooks/useFeature';
import { UNIT_FEATURE_FLAG } from 'views/components/visualizations/Constants';
import generateDomain from 'views/components/visualizations/utils/generateDomain';

const useChartLayoutSettingsWithCustomUnits = ({ config, barmode, chartData }: {
  config: AggregationWidgetConfig,
  barmode?: BarMode,
  chartData: Array<ChartDefinition>,
}) => {
  const theme = useTheme();
  const unitFeatureEnabled = useFeature(UNIT_FEATURE_FLAG);
  const widgetUnits = useWidgetUnits(config);
  const { unitTypeMapper } = useMemo(() => generateMappersForYAxis({ series: config.series, units: widgetUnits }), [config.series, widgetUnits]);

  return useCallback(() => {
    if (!unitFeatureEnabled) return ({});

    const generatedLayouts = generateLayouts({
      unitTypeMapper,
      barmode,
      chartData,
      widgetUnits,
      config,
      theme,
    });

    const _layouts: Partial<Layout> = ({
      ...generatedLayouts,
      hovermode: 'x',
      xaxis: { domain: generateDomain(Object.keys(unitTypeMapper)?.length) },
    });

    return _layouts;
  }, [barmode, chartData, config, theme, unitFeatureEnabled, unitTypeMapper, widgetUnits]);
};

export default useChartLayoutSettingsWithCustomUnits;
