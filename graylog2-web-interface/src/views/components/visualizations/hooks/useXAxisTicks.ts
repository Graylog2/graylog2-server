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
import { useMemo } from 'react';
import flatMap from 'lodash/flatMap';
import compact from 'lodash/compact';

import useMapKeys from 'views/components/visualizations/useMapKeys';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import { keySeparator, humanSeparator } from 'views/Constants';

const useXAxisTicks = (config: AggregationWidgetConfig, chartData: Array<ChartDefinition>) => {
  const mapKeys = useMapKeys();

  return useMemo(() => {
    if (config.isTimeline) return {};
    const tickvals = compact(flatMap(chartData, 'x'));

    const rowPivotFields = config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [];

    return {
      tickvals,
      ticktext: tickvals.map((label) =>
        label
          .split(keySeparator)
          .map((l, i) => mapKeys(l, rowPivotFields[i]))
          .join(humanSeparator),
      ),
    };
  }, [chartData, config.isTimeline, config?.rowPivots, mapKeys]);
};

export default useXAxisTicks;
