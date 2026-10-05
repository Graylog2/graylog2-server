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

type Fixture = { [key: string]: Rows };

export const twoRowPivots: Fixture = {
  chart: [
    {
      key: ['a1', 'b1'],
      values: [{ key: ['count()'], value: 5, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    },
    {
      key: ['a1', 'b2'],
      values: [{ key: ['count()'], value: 3, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    },
    {
      key: ['a2', 'b1'],
      values: [{ key: ['count()'], value: 7, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    },
  ],
};

export const sharedValue: Fixture = {
  chart: [
    {
      key: ['x', 'y'],
      values: [{ key: ['count()'], value: 4, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    },
    {
      key: ['y', 'x'],
      values: [{ key: ['count()'], value: 2, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    },
  ],
};

export const threeGroupings: Fixture = {
  chart: [
    {
      key: ['a1', 'b1'],
      values: [
        { key: ['c1', 'count()'], value: 2, rollup: false, source: 'col-leaf' },
        { key: ['c2', 'count()'], value: 3, rollup: false, source: 'col-leaf' },
      ],
      source: 'leaf',
    },
    {
      key: ['a1', 'b2'],
      values: [{ key: ['c1', 'count()'], value: 4, rollup: false, source: 'col-leaf' }],
      source: 'leaf',
    },
  ],
};

export const twoRowPivotsNoMetric: Fixture = {
  chart: [
    { key: ['a1', 'b1'], values: [], source: 'leaf' },
    { key: ['a2', 'b2'], values: [], source: 'leaf' },
  ],
};
