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
import moment from 'moment';

import { createDefaultTimestampFilter } from './defaultFilters';
import { DATE_SEPARATOR } from './timeRange';

describe('createDefaultTimestampFilter', () => {
  it('should return OrderedMap with timestamp key', () => {
    const result = createDefaultTimestampFilter();

    expect(result.has('timestamp')).toBe(true);
  });

  it('should use default 30 days when no parameter provided', () => {
    const result = createDefaultTimestampFilter();
    const timestampFilter = result.get('timestamp')[0];
    const [fromDate] = timestampFilter.split(DATE_SEPARATOR);
    const parsedDate = moment.utc(fromDate);
    const expectedDate = moment.utc().subtract(30, 'days');
    const diffInHours = expectedDate.diff(parsedDate, 'hours');

    expect(Math.abs(diffInHours)).toBeLessThan(1);
  });

  it('should accept custom day count', () => {
    const result = createDefaultTimestampFilter(7);
    const timestampFilter = result.get('timestamp')[0];
    const [fromDate] = timestampFilter.split(DATE_SEPARATOR);
    const parsedDate = moment.utc(fromDate);
    const expectedDate = moment.utc().subtract(7, 'days');
    const diffInHours = expectedDate.diff(parsedDate, 'hours');

    expect(Math.abs(diffInHours)).toBeLessThan(1);
  });

  it('should format timestamp in ISO 8601 UTC format', () => {
    const result = createDefaultTimestampFilter();
    const timestampFilter = result.get('timestamp')[0];

    expect(timestampFilter).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}\+00:00><$/);
  });

  it('should use DATE_SEPARATOR format', () => {
    const result = createDefaultTimestampFilter();
    const timestampFilter = result.get('timestamp')[0];

    expect(timestampFilter).toContain(DATE_SEPARATOR);
    expect(timestampFilter.endsWith(DATE_SEPARATOR)).toBe(true);
  });

  it('should use empty string after separator to indicate "until now"', () => {
    const result = createDefaultTimestampFilter();
    const timestampFilter = result.get('timestamp')[0];
    const [, untilDate] = timestampFilter.split(DATE_SEPARATOR);

    expect(untilDate).toBe('');
  });

  it('should work with various day counts', () => {
    const testCases = [1, 7, 14, 30, 60, 90, 365];

    testCases.forEach((days) => {
      const result = createDefaultTimestampFilter(days);
      const timestampFilter = result.get('timestamp')[0];
      const [fromDate] = timestampFilter.split(DATE_SEPARATOR);
      const parsedDate = moment.utc(fromDate);
      const expectedDate = moment.utc().subtract(days, 'days');
      const diffInHours = expectedDate.diff(parsedDate, 'hours');

      expect(Math.abs(diffInHours)).toBeLessThan(1);
    });
  });
});
