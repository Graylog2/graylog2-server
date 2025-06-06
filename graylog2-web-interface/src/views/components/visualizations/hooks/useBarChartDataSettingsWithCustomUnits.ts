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
import { useCallback } from 'react';

import useFeature from 'hooks/useFeature';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { getBarChartTraceOffsetGroup } from 'views/components/visualizations/utils/chartLayoutGenerators';
import type { BarMode } from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import { UNIT_FEATURE_FLAG } from 'views/components/visualizations/Constants';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import useChartDataSettingsWithCustomUnits from 'views/components/visualizations/hooks/useChartDataSettingsWithCustomUnits';

const useBarChartDataSettingsWithCustomUnits = ({
  config,
  barmode,
}: {
  config: AggregationWidgetConfig;
  barmode?: BarMode;
}) => {
  const unitFeatureEnabled = useFeature(UNIT_FEATURE_FLAG);
  const getChartDataSettingsWithCustomUnits = useChartDataSettingsWithCustomUnits({ config });

  return useCallback(
    ({
      values,
      idx,
      fullPath,
      name,
    }: {
      xAxisItemsLength: number;
      originalName: string;
      name: string;
      values: Array<any>;
      idx: number;
      total: number;
      fullPath: string;
    }): Partial<ChartDefinition> => {
      if (!unitFeatureEnabled) return {};

      const {
        y: convertedValues,
        yaxis,
        ...hoverTemplateSettings
      } = getChartDataSettingsWithCustomUnits({ name, fullPath, values });

      const offsetgroup = getBarChartTraceOffsetGroup(barmode, yaxis, idx);

      return {
        yaxis,
        y: convertedValues,
        fullPath,
        offsetgroup,
        ...hoverTemplateSettings,
      };
    },
    [barmode, getChartDataSettingsWithCustomUnits, unitFeatureEnabled],
  );
};

export default useBarChartDataSettingsWithCustomUnits;
