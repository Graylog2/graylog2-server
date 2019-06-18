// @flow strict
import { readFileSync } from 'fs';
import { dirname } from 'path';
import md5 from 'md5';
import { flow } from 'lodash';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { chartData, extractSeries, generateChart } from '../ChartData';
import transformKeys from '../TransformKeys';

const cwd = dirname(__filename);
const readFixture = filename => JSON.parse(readFileSync(`${cwd}/${filename}`, 'utf-8'));

describe('Chart helper functions', () => {
  const config = AggregationWidgetConfig.builder().build();

  describe('chartData', () => {
    it('should not fail for empty data', () => {
      const result = chartData(config, [], 'dummy');
      expect(result).toHaveLength(0);
    });
    it('should properly extract series from simplest fixture with one series and one row pivot', () => {
      const input = readFixture('ChartData.test.simplest.json');
      const result = chartData(config, input, 'dummy');
      const expectedResult = [{
        name: 'count()',
        type: 'dummy',
        x: ['index', 'show', 'login', 'edit'],
        y: [27142, 7826, 6626, 1246],
      }];
      expect(result).toHaveLength(1);
      expect(result).toEqual(expectedResult);
    });
    it('should properly extract series from simplest fixture and include provided chart type', () => {
      const input = readFixture('ChartData.test.simplest.json');
      const result = chartData(config, input, 'bar');
      const expectedResult = [{
        name: 'count()',
        type: 'bar',
        x: ['index', 'show', 'login', 'edit'],
        y: [27142, 7826, 6626, 1246],
      }];
      expect(result).toHaveLength(1);
      expect(result).toEqual(expectedResult);
    });
    it('should remove non-present data points and leave order of values intact', () => {
      const input = readFixture('ChartData.test.withHoles.json');
      const result = chartData(config, input, 'bar');
      const expectedResult = [{
        name: 'count()',
        type: 'bar',
        x: [
          '2018-05-28T11:48:00.000Z',
          '2018-05-28T11:49:00.000Z',
          '2018-05-28T11:50:00.000Z',
          '2018-05-28T11:52:00.000Z',
          '2018-05-28T11:53:00.000Z',
        ],
        y: [7813, 8657, 8645, 8630, 702],
      }, {
        name: 'sum(took_ms)',
        type: 'bar',
        x: [
          '2018-05-28T11:48:00.000Z',
          '2018-05-28T11:50:00.000Z',
          '2018-05-28T11:51:00.000Z',
          '2018-05-28T11:52:00.000Z',
          '2018-05-28T11:53:00.000Z',
        ],
        y: [587008, 646728, 792102, 579708, 62596],
      }];
      expect(result).toHaveLength(2);
      expect(result).toEqual(expectedResult);
    });
    it('should properly extract series from fixture with two column pivots', () => {
      const input = readFixture('ChartData.test.twoColumnPivots.json');
      const result = chartData(config, input, 'dummy');
      const expectedResult = readFixture('ChartData.test.twoColumnPivots.result.json');
      expect(result).toHaveLength(6);
      expect(result).toEqual(expectedResult);
    });
    it('should include chart type in result', () => {
      const input = readFixture('ChartData.test.simple.json');
      const result = chartData(config, input, 'scatter');
      const expectedResult = readFixture('ChartData.test.simple.result.json');
      expect(result).toHaveLength(6);
      expect(result).toEqual(expectedResult);
    });
  });
  describe('generateChart', () => {
    it('should allow passing a generator function modelling the chart config', () => {
      const input = readFixture('ChartData.test.simple.json');
      const generatorFunction = (type, name, labels, values) => md5(JSON.stringify({ type, name, labels, values }));
      const pipeline = flow([
        transformKeys(config.rowPivots, config.columnPivots),
        extractSeries(),
        // $FlowFixMe: Returning different result type on purpose
        generateChart('scatter', generatorFunction),
      ]);
      const result = pipeline(input);
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
});
