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
import type { Key, Row } from 'views/logic/searchtypes/pivot/PivotHandler';

export type LeafPath = { keys: Array<Key>; value: number };

/**
 * Walk a pivot result and extract per-row paths.
 *
 * When `metricName` is given, the path's value is the matching metric's value
 * (only positive numeric values are kept). When undefined, each emitted leaf
 * contributes a static weight of 1.
 *
 * Each path's `keys` concatenates the outer row-pivot keys with the column-pivot
 * keys (column-leaves drop the trailing metric-name segment).
 */
const extractLeafPaths = (
  rows: ReadonlyArray<Row>,
  columnFieldCount: number,
  metricName: string | undefined,
): Array<LeafPath> => {
  const paths: Array<LeafPath> = [];

  rows.forEach((row) => {
    if (row.source !== 'leaf') return;

    if (metricName === undefined) {
      const colChildren = (row.values ?? []).filter((v) => v.source === 'col-leaf');

      if (columnFieldCount > 0 && colChildren.length > 0) {
        colChildren.forEach((value) => {
          const colKeys = value.key.length === columnFieldCount + 1 ? value.key.slice(0, -1) : value.key;

          if (colKeys.length !== columnFieldCount) return;

          paths.push({
            keys: [...row.key, ...colKeys],
            value: 1,
          });
        });
      } else if (row.key.length > 0) {
        paths.push({ keys: [...row.key], value: 1 });
      }

      return;
    }

    row.values.forEach((value) => {
      if (value.source !== 'col-leaf' && value.source !== 'row-leaf') return;
      if (value.key.length !== columnFieldCount + 1) return;
      if (value.key[value.key.length - 1] !== metricName) return;

      const numericValue = Number(value.value);

      if (!Number.isFinite(numericValue) || numericValue <= 0) return;

      paths.push({
        keys: [...row.key, ...value.key.slice(0, -1)],
        value: numericValue,
      });
    });
  });

  return paths;
};

export default extractLeafPaths;
