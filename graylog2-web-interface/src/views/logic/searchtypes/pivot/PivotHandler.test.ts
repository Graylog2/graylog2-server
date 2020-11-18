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
// @flow strict

import PivotHandler from './PivotHandler';
import type { Result } from './PivotHandler';

const pivotResult: Result = {
  id: '9c66f71e-fec9-48be-9496-20ad982c07a5',
  rows: [{
    key: ['US', 'Seattle'],
    values: [{ key: ['TCP', 'count()'], value: 18, rollup: false, source: 'col-leaf' }, {
      key: ['count()'],
      value: 18,
      rollup: true,
      source: 'row-leaf',
    }],
    source: 'leaf',
  }, {
    key: ['US', 'Mountain View'],
    values: [{
      key: ['TCP', 'count()'],
      value: 2,
      rollup: false,
      source: 'col-leaf',
    }, { key: ['UDP', 'count()'], value: 2, rollup: false, source: 'col-leaf' }, {
      key: ['count()'],
      value: 4,
      rollup: true,
      source: 'row-leaf',
    }],
    source: 'leaf',
  }, {
    key: ['US'],
    values: [{ key: ['count()'], value: 22, rollup: true, source: 'row-inner' }],
    source: 'non-leaf',
  }, {
    key: ['DE', 'Berlin'],
    values: [{ key: ['TCP', 'count()'], value: 19, rollup: false, source: 'col-leaf' }, {
      key: ['count()'],
      value: 19,
      rollup: true,
      source: 'row-leaf',
    }],
    source: 'leaf',
  }, {
    key: ['DE', 'Bochum'],
    values: [{ key: ['UDP', 'count()'], value: 2, rollup: false, source: 'col-leaf' }, {
      key: ['count()'],
      value: 2,
      rollup: true,
      source: 'row-leaf',
    }],
    source: 'leaf',
  }, {
    key: ['DE'],
    values: [{ key: ['count()'], value: 21, rollup: true, source: 'row-inner' }],
    source: 'non-leaf',
  }, {
    key: ['AU', 'Riverton'],
    values: [{ key: ['TCP', 'count()'], value: 6, rollup: false, source: 'col-leaf' }, {
      key: ['count()'],
      value: 6,
      rollup: true,
      source: 'row-leaf',
    }],
    source: 'leaf',
  }, {
    key: ['AU'],
    values: [{ key: ['count()'], value: 6, rollup: true, source: 'row-inner' }],
    source: 'non-leaf',
  }, {
    key: [],
    values: [{ key: ['count()'], value: 539, rollup: true, source: 'row-inner' }],
    source: 'non-leaf',
  }],
  total: 539,
  type: 'pivot',
};

describe('PivotHandler', () => {
  it('has type information matching actual result', () => {
    PivotHandler.convert(pivotResult);
  });
});
