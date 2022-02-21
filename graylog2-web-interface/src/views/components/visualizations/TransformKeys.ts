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
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import type { ColLeaf, Leaf, Key, Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { DateTime, DateTimeFormats } from 'util/DateTime';
import { isValidDate } from 'util/DateTime';

type TimeFormatter = (time: DateTime, format?: DateTimeFormats) => string;

const transformKey = (key: Key, indices: Array<number>, formatTimestamp: TimeFormatter) => {
  if (indices.length === 0) {
    return key;
  }

  const newKey: Key = key.slice();

  indices.forEach((idx) => {
    if (newKey[idx]) {
      const value = newKey[idx];
      newKey[idx] = isValidDate(value) ? formatTimestamp(newKey[idx], 'internal') : value;
    }
  });

  return newKey;
};

const findIndices = <T> (ary: Array<T>, predicate: (value: T) => boolean): Array<number> => ary
  .map((value, idx) => ({ value, idx }))
  .filter(({ value }) => predicate(value))
  .map(({ idx }) => idx);

export default (rowPivots: Array<Pivot>, columnPivots: Array<Pivot>, formatTime: TimeFormatter): (rows: Rows) => Rows => {
  return (result = []) => {
    const rowIndices = findIndices(rowPivots, (pivot) => (pivot.type === 'time'));
    const columnIndices = findIndices(columnPivots, (pivot) => (pivot.type === 'time'));

    if (rowIndices.length === 0 && columnIndices.length === 0) {
      return result;
    }

    return result.map((row) => {
      if (row.source !== 'leaf') {
        return row;
      }

      const newRow: Leaf = { ...row };

      newRow.key = transformKey(row.key, rowIndices, formatTime);

      if (columnIndices.length > 0) {
        newRow.values = row.values.map((values) => {
          if (values.source !== 'col-leaf') {
            return values;
          }

          const newValues: ColLeaf = { ...values };

          newValues.key = transformKey(values.key, columnIndices, formatTime);

          return newValues;
        });
      }

      return newRow;
    });
  };
};
