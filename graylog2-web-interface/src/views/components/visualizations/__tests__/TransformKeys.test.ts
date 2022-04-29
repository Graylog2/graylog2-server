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
import moment from 'moment-timezone';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';

import * as fixtures from './TransformKeys.fixtures';

import transformKeys from '../TransformKeys';

const formatTime = (timestamp: string) => timestamp;
const formatTimeForLocalTz = (timezone: string) => (timestamp: string) => moment.tz(timestamp, timezone).toISOString(true);

// eslint-disable-next-line global-require
describe('TransformKeys', () => {
  it('returns original result when no aggregations are present', () => {
    const rows: Rows = [{
      source: 'row-leaf',
      value: 42,
      key: ['foo'],
      rollup: false,
    }];
    const result = transformKeys([], [], formatTime)(rows);

    expect(result).toEqual(rows);
  });

  it('returns original result when no time aggregations are present', () => {
    const rows: Rows = [{
      source: 'row-leaf',
      value: 42,
      key: ['foo'],
      rollup: false,
    }];
    const result = transformKeys([Pivot.create('foo', 'value')], [Pivot.create('bar', 'value')], formatTime)(rows);

    expect(result).toEqual(rows);
  });

  it('transforms row keys using current user\'s timezone', () => {
    const input: Rows = [
      {
        source: 'leaf',
        key: ['2018-10-01T15:10:55.323Z'],
        values: [],
      },
      {
        source: 'leaf',
        key: ['2017-03-12T09:32:21.283-08:00'],
        values: [],
      },
    ];

    const result = transformKeys([Pivot.create('timestamp', 'time')], [], formatTimeForLocalTz('Europe/Berlin'))(input);

    expect(result).toEqual([
      {
        key: ['2018-10-01T17:10:55.323+02:00'],
        source: 'leaf',
        values: [],
      }, {
        key: ['2017-03-12T18:32:21.283+01:00'],
        source: 'leaf',
        values: [],
      },
    ]);
  });

  it('transforms column keys using current user\'s timezone', () => {
    const input: Rows = [
      {
        source: 'leaf',
        key: ['2018-10-01T15:10:55.323Z'],
        values: [],
      },
      {
        source: 'leaf',
        key: ['2017-03-12T09:32:21.283-08:00'],
        values: [],
      },
    ];

    const result = transformKeys([Pivot.create('timestamp', 'time')], [], formatTimeForLocalTz('America/Los_Angeles'))(input);

    expect(result).toEqual([
      {
        key: ['2018-10-01T08:10:55.323-07:00'],
        source: 'leaf',
        values: [],
      }, {
        key: ['2017-03-12T10:32:21.283-07:00'],
        source: 'leaf',
        values: [],
      },
    ]);
  });

  it('transforms complete results using current user\'s timezone', () => {
    const { rowPivots, columnPivots, input, output } = fixtures.singleRowPivot;
    const result = transformKeys(rowPivots as Pivot[], columnPivots, formatTimeForLocalTz('America/New_York'))(input);

    expect(result).toEqual(output);
  });

  it('does not transform complete results without time pivots', () => {
    const { rowPivots, columnPivots, input, output } = fixtures.noTimePivots;
    const result = transformKeys(rowPivots as Pivot[], columnPivots as Pivot[], formatTime)(input);

    expect(result).toEqual(output);
  });
});
