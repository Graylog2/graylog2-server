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
import flatMap from 'lodash/flatMap';
import compact from 'lodash/compact';
import type { LayoutAxis } from 'plotly.js';

import useMapKeys from 'views/components/visualizations/useMapKeys';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import { keySeparator, humanSeparator } from 'views/Constants';
import useQueryFieldTypes from 'views/hooks/useQueryFieldTypes';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';

const useXAxisTicksAndType = (
  config: AggregationWidgetConfig,
  chartData: Array<ChartDefinition>,
): Partial<LayoutAxis> => {
  const mapKeys = useMapKeys();
  const fieldTypes = useQueryFieldTypes();

  if (config.isTimeline) return {};
  const tickvals = compact(flatMap(chartData, 'x'));
  const rowPivotFields = config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [];

  const isCategoryType =
    rowPivotFields.length === 1 &&
    (!fieldTypeFor(rowPivotFields[0], fieldTypes).isNumeric() ||
      tickvals.some((v) => {
        const convertedValue = Number(v);

        return Number.isNaN(convertedValue) || !Number.isInteger(convertedValue);
      }));

  return {
    tickvals,
    ticktext: tickvals.map((label) =>
      label
        .split(keySeparator)
        .map((l, i) => mapKeys(l, rowPivotFields[i]))
        .join(humanSeparator),
    ),
    type: isCategoryType ? 'category' : undefined,
  };
};

export default useXAxisTicksAndType;
