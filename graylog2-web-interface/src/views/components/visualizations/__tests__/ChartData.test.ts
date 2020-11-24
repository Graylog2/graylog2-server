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
import { readFileSync } from 'fs';

import { dirname } from 'path';
import md5 from 'md5';
import { flow, merge, fill } from 'lodash';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import { chartData, ExtractedSeries, extractSeries, formatSeries, generateChart, ValuesBySeries } from '../ChartData';
import transformKeys from '../TransformKeys';

const cwd = dirname(__filename);
const readFixture = (filename) => JSON.parse(readFileSync(`${cwd}/${filename}`, 'utf-8'));

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

    it('should not remove data points with a value of zero', () => {
      const input = readFixture('ChartData.test.withZeros.json');
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
        y: [7813, 0, 0, 0, 702],
      }];

      expect(result).toHaveLength(1);
      expect(result).toEqual(expectedResult);
    });

    it('should remove data points with a value of null or undefined', () => {
      const input = readFixture('ChartData.test.withNullAndUndefined.json');
      const result = chartData(config, input, 'bar');
      const expectedResult = [{
        name: 'count()',
        type: 'bar',
        x: [
          '2018-05-28T11:48:00.000Z',
          '2018-05-28T11:53:00.000Z',
        ],
        y: [7813, 702],
      }];

      expect(result).toHaveLength(1);
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

    it('should allow passing a format series function to modify the series structure', () => {
      const input = readFixture('ChartData.test.oneColumOneRowPivot.json');
      const generatorFunction = (type, name, x, y, z) => ({ type, name, x, y, z });

      const formatSeriesCustom = ({ valuesBySeries, xLabels }: { valuesBySeries: ValuesBySeries, xLabels: Array<any> }): ExtractedSeries => {
        // In this example we want to create only one series, with an z value, which contains all series data
        const z: Array<any> = Object.values(valuesBySeries).map((series) => {
          const newSeries = fill(Array(xLabels.length), null);

          return merge(newSeries, series);
        });
        const yLabels = Object.keys(valuesBySeries);

        return [[
          'XYZ Chart',
          xLabels,
          yLabels,
          z,
        ]];
      };

      const result = chartData(config, input, 'heatmap', generatorFunction, formatSeriesCustom);
      const expectedResult = readFixture('ChartData.test.oneColumOneRowPivot.result.json');

      expect(result).toHaveLength(1);
      expect(result).toEqual(expectedResult);
    });

    it('should allow passing a leaf source matcher function to modify the resulting series', () => {
      const input = readFixture('ChartData.test.simple.json');
      const leafSourceMatcher = ({ source }) => source.endsWith('leaf') && source !== 'row-leaf';
      const result = chartData(config, input, 'scatter', undefined, undefined, leafSourceMatcher);
      const expectedResult = readFixture('ChartData.test.simple.sourceMatcher.result.json');

      expect(result).toHaveLength(4);
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
        formatSeries,
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
