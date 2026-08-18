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

import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import extractLeafPaths from 'views/components/visualizations/utils/extractLeafPaths';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import { buildStages } from './buildStages';
import type { FunnelStage } from './types';

type Result = {
  stages: FunnelStage[] | null;
  rowFields: string[];
  colFields: string[];
};

const useFunnelStages = (config: AggregationWidgetConfig, data: VisualizationComponentProps['data']): Result => {
  const rows = retrieveChartData(data);
  const mapKeys = useMapKeys();

  return useMemo(() => {
    const rowFields = config.rowPivots.flatMap((p) => p.fields);
    const colFields = config.columnPivots.flatMap((p) => p.fields);
    const allFields = [...rowFields, ...colFields];

    if (allFields.length === 0 || !rows) return { stages: null, rowFields, colFields };

    const metric = config.series?.[0];
    const paths = extractLeafPaths(rows, colFields.length, metric?.effectiveName);

    if (paths.length === 0) return { stages: null, rowFields, colFields };

    const displayKeys = paths.map((path) =>
      path.keys.map((k, i) => String(mapKeys(k, allFields[i]) ?? k)),
    );

    return { stages: buildStages(paths, displayKeys, rowFields.length), rowFields, colFields };
  }, [config, mapKeys, rows]);
};

export default useFunnelStages;
