import { readFileSync } from 'fs';
import { dirname } from 'path';

import expandRows from './ExpandRows';

const cwd = dirname(__filename);
const readFixture = (filename) => JSON.parse(readFileSync(`${cwd}/${filename}`));

describe('ExpandRows', () => {
  it('normalizes empty array', () => {
    const data = [];
    const result = expandRows(['controller'], [], data);
    expect(result).toHaveLength(0);
    expect(result).toEqual([]);
  });
  it('does not throw exception when data is undefined', () => {
    const data = undefined;
    const result = expandRows(['controller'], [], data);
    expect(result).toHaveLength(0);
    expect(result).toEqual([]);
  });

  it('properly expands a simple result from a pivot search type', () => {
    const pivotResult = readFixture('ExpandRows.test.simple.json');
    const result = expandRows(['timestamp', 'controller'], ['action'], pivotResult);
    expect(result).toHaveLength(2);
    expect(result).toEqual([
      {
        action: {
          edit: {
            'count()': 118,
            'sum(took_ms)': 6580,
          },
          index: {
            'count()': 2696,
            'sum(took_ms)': 239622,
          },
        },
        controller: 'PostsController',
        'count()': 3680,
        'sum(took_ms)': 327284,
        timestamp: '2018-05-24T14:03:00.000Z',
      }, {
        'count()': 4490,
        'sum(took_ms)': 384742,
        timestamp: '2018-05-24T14:03:00.000Z',
      },
    ]);
  });
  it('properly expands a given result from a pivot search type', () => {
    const pivotResult = readFixture('ExpandRows.test.fixture1.json');
    const result = expandRows(['timestamp', 'controller'], ['action'], pivotResult);
    const expectedResult = readFixture('ExpandRows.test.fixture1.result.json');
    expect(result).toHaveLength(25);
    expect(result).toEqual(expectedResult);
  });
  it('properly expands a result with two column pivots', () => {
    const pivotResult = readFixture('ExpandRows.test.twoColumnPivots.json');
    const result = expandRows(['timestamp'], ['action', 'controller'], pivotResult);
    const expectedResult = readFixture('ExpandRows.test.twoColumnPivots.result.json');
    expect(result).toHaveLength(7);
    expect(result).toEqual(expectedResult);
  });
  it('properly expands a result with no pivots but series', () => {
    const pivotResult = readFixture('ExpandRows.test.noPivots.json');
    const result = expandRows([], [], pivotResult);
    expect(result).toHaveLength(1);
    expect(result).toEqual([{
      'avg(took_ms)': 80.65076335877863,
      'min(took_ms)': 36,
      'max(took_ms)': 5850,
      'sum(took_ms)': 3465402,
      'stddev(took_ms)': 331.44857900293187,
    }]);
  });
});
