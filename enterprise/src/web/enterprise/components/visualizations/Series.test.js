// @flow strict
import { readFileSync } from 'fs';
import { dirname } from 'path';
import md5 from 'md5';

import { generateSeries } from './Series';

const cwd = dirname(__filename);
const readFixture = filename => JSON.parse(readFileSync(`${cwd}/${filename}`, 'utf-8'));

describe('Series helper functions', () => {
  it('generateSeries should not fail for empty data', () => {
    const result = generateSeries([], 'dummy');
    expect(result).toHaveLength(0);
  });
  it('generateSeries should properly extract series from simplest fixture with one series and one row pivot', () => {
    const input = readFixture('Series.test.simplest.json');
    const result = generateSeries(input, 'dummy');
    const expectedResult = [{
      name: 'count()',
      type: 'dummy',
      x: ['index', 'show', 'login', 'edit'],
      y: [27142, 7826, 6626, 1246],
    }];
    expect(result).toHaveLength(1);
    expect(result).toEqual(expectedResult);
  });
  it('generateSeries should properly extract series from simplest fixture and include provided chart type', () => {
    const input = readFixture('Series.test.simplest.json');
    const result = generateSeries(input, 'bar');
    const expectedResult = [{
      name: 'count()',
      type: 'bar',
      x: ['index', 'show', 'login', 'edit'],
      y: [27142, 7826, 6626, 1246],
    }];
    expect(result).toHaveLength(1);
    expect(result).toEqual(expectedResult);
  });
  it('generateSeries should mark non-present data points and leave order of values intact', () => {
    const input = readFixture('Series.test.withHoles.json');
    const result = generateSeries(input, 'bar');
    const timestamps = [
      '2018-05-28T11:48:00.000Z',
      '2018-05-28T11:49:00.000Z',
      '2018-05-28T11:50:00.000Z',
      '2018-05-28T11:51:00.000Z',
      '2018-05-28T11:52:00.000Z',
      '2018-05-28T11:53:00.000Z',
    ];
    const expectedResult = [{
      name: 'count()',
      type: 'bar',
      x: timestamps,
      y: [7813, 8657, 8645, undefined, 8630, 702],
    }, {
      name: 'sum(took_ms)',
      type: 'bar',
      x: timestamps,
      y: [587008, undefined, 646728, 792102, 579708, 62596],
    }];
    expect(result).toHaveLength(2);
    expect(result).toEqual(expectedResult);
  });
  it('generateSeries should properly extract series from fixture with two column pivots', () => {
    const input = readFixture('Series.test.twoColumnPivots.json');
    const result = generateSeries(input, 'dummy');
    const expectedResult = readFixture('Series.test.twoColumnPivots.result.json');
    expect(result).toHaveLength(6);
    expect(result).toEqual(expectedResult);
  });
  it('generateSeries should include chart type in result', () => {
    const input = readFixture('Series.test.simple.json');
    const result = generateSeries(input, 'scatter');
    const expectedResult = readFixture('Series.test.simple.result.json');
    expect(result).toHaveLength(6);
    expect(result).toEqual(expectedResult);
  });
  it('generateSeries should allow passing a generator function modelling the chart config', () => {
    const input = readFixture('Series.test.simple.json');
    const generatorFunction = (type, name, labels, values) => md5(JSON.stringify({ type, name, labels, values }));
    // $FlowFixMe: Returning different result type on purpose
    const result = generateSeries(input, 'scatter', generatorFunction);
    const expectedResult = [
      '99fff4aaa8e33abf060756997b07172c',
      '07493899371a4a8b67c14a305774f9d9',
      '1846191e09cf20e5f2090abeb01877a7',
      '8e560cc5648d21674230dfbb5e99f4d7',
      '57469e98b570e77672233b258c7d91a0',
      '88648c9ca14f65ef199856a4fda8836e',
    ];
    expect(result).toHaveLength(6);
    expect(result).toEqual(expectedResult);
  });
});
