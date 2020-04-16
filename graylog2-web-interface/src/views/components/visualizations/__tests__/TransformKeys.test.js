// @flow strict
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import CurrentUserStore from 'stores/users/CurrentUserStore';

import transformKeys from '../TransformKeys';
import * as fixtures from './TransformKeys.fixtures';

jest.mock('stores/users/CurrentUserStore', () => ({ get: jest.fn() }));

// eslint-disable-next-line global-require
describe('TransformKeys', () => {
  it('returns original result when no aggregations are present', () => {
    const rows = [{
      source: 'row-leaf',
      value: 42,
      key: ['foo'],
      rollup: false,
    }];
    const result = transformKeys([], [])(rows);
    expect(result).toEqual(rows);
  });

  it('returns original result when no time aggregations are present', () => {
    const rows = [{
      source: 'row-leaf',
      value: 42,
      key: ['foo'],
      rollup: false,
    }];
    const result = transformKeys([Pivot.create('foo', 'value')], [Pivot.create('bar', 'value')])(rows);
    expect(result).toEqual(rows);
  });

  it('transforms row keys using current user\'s timezone', () => {
    CurrentUserStore.get.mockReturnValue({ timezone: 'Europe/Berlin' });
    const input = [
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

    const result = transformKeys([Pivot.create('timestamp', 'time')], [])(input);

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
    CurrentUserStore.get.mockReturnValueOnce({ timezone: 'America/Los_Angeles' });
    const input = [
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

    const result = transformKeys([Pivot.create('timestamp', 'time')], [])(input);

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

  it('transforms column keys using AppConfig rootTimeZone if user\'s timezone is null', () => {
    CurrentUserStore.get.mockReturnValueOnce({ timezone: null });

    const input = [
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

    const result = transformKeys([Pivot.create('timestamp', 'time')], [])(input);

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

  it('transforms complete results using current user\'s timezone', () => {
    CurrentUserStore.get.mockReturnValueOnce({ timezone: 'America/New_York' });
    const { rowPivots, columnPivots, input, output } = fixtures.singleRowPivot;
    const result = transformKeys(rowPivots, columnPivots)(input);

    expect(result).toEqual(output);
  });

  it('does not transform complete results without time pivots', () => {
    const { rowPivots, columnPivots, input, output } = fixtures.noTimePivots;
    const result = transformKeys(rowPivots, columnPivots)(input);

    expect(result).toEqual(output);
  });
});
