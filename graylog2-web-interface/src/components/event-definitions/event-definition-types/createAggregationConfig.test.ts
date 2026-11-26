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
import createAggregationConfig from './createAggregationConfig';

describe('createAggregationConfig', () => {
  it('builds the series and condition for a function of a field', () => {
    expect(
      createAggregationConfig({
        function: 'avg',
        field: 'took_ms',
        operator: '>=',
        threshold: 500,
        group_by: ['source'],
      }),
    ).toEqual({
      group_by: ['source'],
      series: [{ id: 'avg-took_ms', type: 'avg', field: 'took_ms' }],
      conditions: {
        expression: {
          expr: '>=',
          left: { expr: 'number-ref', ref: 'avg-took_ms' },
          right: { expr: 'number', value: 500 },
        },
      },
    });
  });

  it('uses no field and no grouping when none are given', () => {
    expect(createAggregationConfig({ function: 'count', field: '', operator: '>', threshold: 10 })).toEqual(
      expect.objectContaining({ group_by: [], series: [{ id: 'count-', type: 'count', field: null }] }),
    );
  });
});
