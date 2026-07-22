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
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';

import extractLeafPaths from './extractLeafPaths';

describe('extractLeafPaths', () => {
  it('extracts row-leaf values when no column pivots exist', () => {
    const rows: Rows = [
      {
        key: ['a1', 'b1'],
        values: [{ key: ['count()'], value: 5, rollup: true, source: 'row-leaf' }],
        source: 'leaf',
      },
      {
        key: ['a2', 'b1'],
        values: [{ key: ['count()'], value: 7, rollup: true, source: 'row-leaf' }],
        source: 'leaf',
      },
      {
        key: [],
        values: [{ key: ['count()'], value: 12, rollup: true, source: 'row-inner' }],
        source: 'non-leaf',
      },
    ];

    expect(extractLeafPaths(rows, 0, 'count()')).toEqual([
      { keys: ['a1', 'b1'], value: 5 },
      { keys: ['a2', 'b1'], value: 7 },
    ]);
  });

  it('combines row and column pivot keys for col-leaf values', () => {
    const rows: Rows = [
      {
        key: ['a1'],
        values: [
          { key: ['b1', 'count()'], value: 5, rollup: false, source: 'col-leaf' },
          { key: ['count()'], value: 5, rollup: true, source: 'row-leaf' },
        ],
        source: 'leaf',
      },
    ];

    expect(extractLeafPaths(rows, 1, 'count()')).toEqual([{ keys: ['a1', 'b1'], value: 5 }]);
  });

  it('drops null, zero, and negative metric values', () => {
    const rows: Rows = [
      {
        key: ['a1', 'b1'],
        values: [{ key: ['count()'], value: 5, rollup: true, source: 'row-leaf' }],
        source: 'leaf',
      },
      {
        key: ['a2', 'b1'],
        values: [{ key: ['count()'], value: null, rollup: true, source: 'row-leaf' }],
        source: 'leaf',
      },
      {
        key: ['a3', 'b1'],
        values: [{ key: ['count()'], value: 0, rollup: true, source: 'row-leaf' }],
        source: 'leaf',
      },
      {
        key: ['a4', 'b1'],
        values: [{ key: ['count()'], value: -3, rollup: true, source: 'row-leaf' }],
        source: 'leaf',
      },
    ];

    expect(extractLeafPaths(rows, 0, 'count()')).toEqual([{ keys: ['a1', 'b1'], value: 5 }]);
  });

  it('emits weight 1 per leaf when no metric is configured', () => {
    const rows: Rows = [
      { key: ['a1', 'b1'], values: [], source: 'leaf' },
      { key: ['a2', 'b2'], values: [], source: 'leaf' },
    ];

    expect(extractLeafPaths(rows, 0, undefined)).toEqual([
      { keys: ['a1', 'b1'], value: 1 },
      { keys: ['a2', 'b2'], value: 1 },
    ]);
  });

  it('skips values whose trailing key does not match the metric name', () => {
    const rows: Rows = [
      {
        key: ['a1', 'b1'],
        values: [
          { key: ['count()'], value: 5, rollup: true, source: 'row-leaf' },
          { key: ['sum(bytes)'], value: 200, rollup: true, source: 'row-leaf' },
        ],
        source: 'leaf',
      },
    ];

    expect(extractLeafPaths(rows, 0, 'count()')).toEqual([{ keys: ['a1', 'b1'], value: 5 }]);
  });
});
