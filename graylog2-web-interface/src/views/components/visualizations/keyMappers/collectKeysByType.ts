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
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { Rows, Row, Key } from 'views/logic/searchtypes/pivot/PivotHandler';

type VisualizationData = { [dataset: string]: Rows | unknown };

const pivotFieldsOf = (pivots: AggregationWidgetConfig['rowPivots']): Array<string> =>
  (pivots ?? []).flatMap((pivot) => pivot.fields);

const collectKeysByType = (
  data: VisualizationData,
  config: AggregationWidgetConfig,
  fieldTypeOf: (field: string) => string | undefined,
  relevantTypes: Set<string>,
): Record<string, Array<string>> => {
  const rowFields = pivotFieldsOf(config?.rowPivots);
  const columnFields = pivotFieldsOf(config?.columnPivots);
  const acc: Record<string, Set<string>> = {};

  const add = (field: string | undefined, value: Key) => {
    if (!field || value === null || value === undefined) {
      return;
    }

    const type = fieldTypeOf(field);

    if (!type || !relevantTypes.has(type)) {
      return;
    }

    (acc[type] ??= new Set<string>()).add(String(value));
  };

  const collectKey = (key: Array<Key> | undefined, fields: Array<string>) =>
    (key ?? []).forEach((value, idx) => add(fields[idx], value));

  const visitRow = (row: Row) => {
    if ('key' in row && Array.isArray(row.key)) {
      collectKey(row.key, rowFields);
    }

    if ('values' in row && Array.isArray(row.values)) {
      row.values.forEach((value) => {
        if (value.source === 'col-leaf') {
          collectKey(value.key, columnFields);
        } else if ('values' in value) {
          visitRow(value);
        }
      });
    }
  };

  Object.entries(data).forEach(([dataset, rows]) => {
    if (dataset === 'events' || !Array.isArray(rows)) {
      return;
    }

    rows.forEach(visitRow);
  });

  return Object.fromEntries(Object.entries(acc).map(([type, values]) => [type, Array.from(values)]));
};

export default collectKeysByType;
