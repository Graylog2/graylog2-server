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

import getAdjustedPlotTimelineRange, {
  findDateRangeFromChartData,
  getChartWidthFromChartDataArray,
} from 'views/components/visualizations/utils/getAdjustedPlotTimelineRange';
import { chartData4Charts, momentXDates } from 'views/components/visualizations/utils/__tests__/fixtures';

describe('getAdjustedPlotTimelineRange module', () => {
  const expectedChartWidth = 5760000;
  const minimalDelta = 5760000 * 0.8;

  describe('getChartWidthFromChartDataArray', () => {
    it('returns chart width', () => {
      const result = getChartWidthFromChartDataArray(chartData4Charts);

      expect(result).toEqual(expectedChartWidth);
    });
  });

  describe('findDateRangeFromChartData return effective time range as minX maxX', () => {
    it('directly if all dates are in between and have enough space in between', () => {
      const result = findDateRangeFromChartData(
        momentXDates,
        '2024-08-11T10:00:00.000+02:00',
        '2024-08-12T18:00:00.000+02:00',
        minimalDelta,
      );

      expect(result).toEqual({
        minX: '2024-08-11T10:00:00.000+02:00',
        maxX: '2024-08-12T18:00:00.000+02:00',
      });
    });

    it('with adjusting dateFrom and dateTo left (previous in data array) value by adding minimal delta', () => {
      const result = findDateRangeFromChartData(
        momentXDates,
        '2024-08-11T18:01:00.000+02:00', // xDates[1] + 1 min
        '2024-08-12T14:01:00.000+02:00', // xDates[N-1] + 1 min
        minimalDelta,
      );

      expect(result).toEqual({
        minX: '2024-08-11T19:16:48.000+02:00', // xDates[1] + minimalDelta,
        maxX: '2024-08-12T15:16:48.000+02:00', // xDates[N-1] + minimalDelta,
      });
    });

    it('with adjusting dateFrom/To right(after in data array) value by subtracting minimal delta', () => {
      const result = findDateRangeFromChartData(
        momentXDates,
        '2024-08-11T17:59:59.000+02:00', // xDates[1] - 1 min
        '2024-08-12T13:59:59.000+02:00', // xDates[N-1] - 1 min
        minimalDelta,
      );

      expect(result).toEqual({
        minX: '2024-08-11T16:43:12.000+02:00', // xDates[0] - minimalDelta,
        maxX: '2024-08-12T12:43:12.000+02:00', // xDates[N-1] - minimalDelta,
      });
    });

    it('with adjusting first and last items in data array when values are inside the range but too close to range', () => {
      const result = findDateRangeFromChartData(
        momentXDates,
        '2024-08-11T15:59:59.000+02:00', // xDates[0] - 1 min
        '2024-08-12T16:01:00.000+02:00', // xDates[N] + 1 min
        minimalDelta,
      );

      expect(result).toEqual({
        minX: '2024-08-11T14:43:12.000+02:00', // xDates[0] - minimalDelta,
        maxX: '2024-08-12T17:16:48.000+02:00', // xDates[N] + minimalDelta,
      });
    });
  });

  describe('getAdjustedPlotTimelineRange', () => {
    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('returns date from and date to if there is no data', () => {
      const result = getAdjustedPlotTimelineRange([], '2023-08-11T10:00:00.000+02:00', '2025-08-12T18:00:00.000+02:00');

      expect(result).toEqual({
        minX: '2023-08-11T10:00:00.000+02:00',
        maxX: '2025-08-12T18:00:00.000+02:00',
      });
    });

    it('return result', () => {
      const result = getAdjustedPlotTimelineRange(
        chartData4Charts,
        '2024-08-11T16:01:00.000+02:00',
        '2024-08-12T16:01:00.000+02:00',
      );

      expect(result).toEqual({
        minX: '2024-08-11T16:48:00.000+02:00',
        maxX: '2024-08-12T16:48:00.000+02:00',
      });
    });
  });
});
