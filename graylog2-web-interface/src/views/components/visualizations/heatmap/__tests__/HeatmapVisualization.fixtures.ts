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

const validData: { [key: string]: Rows } = {
  chart: [
    {
      key: ['00'],
      values: [
        { key: ['100', 'count()'], value: 217, rollup: false, source: 'col-leaf' },
        { key: ['304', 'count()'], value: 213, rollup: false, source: 'col-leaf' },
        { key: ['count()'], value: 430, rollup: true, source: 'row-leaf' },
      ],
      source: 'leaf',
    },
    {
      key: ['01'],
      values: [
        { key: ['405', 'count()'], value: 230, rollup: false, source: 'col-leaf' },
        { key: ['201', 'count()'], value: 217, rollup: false, source: 'col-leaf' },
        { key: ['count()'], value: 447, rollup: true, source: 'row-leaf' },
      ],
      source: 'leaf',
    },
    {
      key: [],
      values: [{ key: ['count()'], value: 877, rollup: true, source: 'row-inner' }],
      source: 'non-leaf',
    },
  ],
};

const dataWithEmptyBuckets: { [key: string]: Rows } = {
  chart: [
    {
      key: ['00'],
      values: [
        { key: ['100', 'count()'], value: 217, rollup: false, source: 'col-leaf' },
        { key: ['201', 'count()'], value: null, rollup: false, source: 'col-leaf' },
      ],
      source: 'leaf',
    },
    {
      key: ['01'],
      values: [
        { key: ['100', 'count()'], value: 230, rollup: false, source: 'col-leaf' },
        { key: ['201', 'count()'], value: null, rollup: false, source: 'col-leaf' },
      ],
      source: 'leaf',
    },
  ],
};

const dataWithTimeColumns: { [key: string]: Rows } = {
  chart: [
    {
      key: ['Execution'],
      values: [
        { key: ['2026-08-31T19:02:40.000Z', 'count()'], value: 6, rollup: false, source: 'col-leaf' },
        { key: ['2026-08-31T19:03:00.000Z', 'count()'], value: 2, rollup: false, source: 'col-leaf' },
      ],
      source: 'leaf',
    },
    {
      key: ['Collection'],
      values: [
        { key: ['2026-08-31T19:02:00.000Z', 'count()'], value: 1, rollup: false, source: 'col-leaf' },
        { key: ['2026-08-31T19:02:40.000Z', 'count()'], value: 5, rollup: false, source: 'col-leaf' },
      ],
      source: 'leaf',
    },
  ],
};

export { validData, dataWithEmptyBuckets, dataWithTimeColumns };
