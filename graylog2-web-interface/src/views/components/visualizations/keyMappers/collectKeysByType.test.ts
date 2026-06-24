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
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';

import collectKeysByType from './collectKeysByType';

const config = AggregationWidgetConfig.builder()
  .rowPivots([Pivot.createValues(['streams'])])
  .columnPivots([Pivot.createValues(['gl2_source_input'])])
  .build();

const fieldTypeOf = (field: string) => ({ streams: 'streams', gl2_source_input: 'input' })[field];

const data = {
  chart: [
    {
      source: 'leaf',
      key: ['stream-a'],
      values: [
        { source: 'col-leaf', key: ['input-1'], rollup: false, value: 5 },
        { source: 'col-leaf', key: ['input-2'], rollup: false, value: 7 },
      ],
    },
    {
      source: 'leaf',
      key: ['stream-b'],
      values: [{ source: 'col-leaf', key: ['input-1'], rollup: false, value: 3 }],
    },
  ],
  events: [{ something: true }],
};

describe('collectKeysByType', () => {
  it('groups distinct pivot key values by relevant field type', () => {
    const result = collectKeysByType(data, config, fieldTypeOf, new Set(['streams', 'input']));

    expect(result.streams.sort()).toEqual(['stream-a', 'stream-b']);
    expect(result.input.sort()).toEqual(['input-1', 'input-2']);
  });

  it('omits field types that are not relevant', () => {
    const result = collectKeysByType(data, config, fieldTypeOf, new Set(['input']));

    expect(result.streams).toBeUndefined();
    expect(result.input.sort()).toEqual(['input-1', 'input-2']);
  });

  it('ignores the events dataset and missing/null keys', () => {
    const withNulls = {
      chart: [{ source: 'leaf', key: [null], values: [] }],
      events: [{ x: 1 }],
    };

    const result = collectKeysByType(withNulls, config, fieldTypeOf, new Set(['streams']));

    expect(result.streams).toBeUndefined();
  });
});
