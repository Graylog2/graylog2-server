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

import { formatDuration, parseDuration, splitDuration } from './duration';

describe('duration', () => {
  it.each([
    ['PT30S', 30],
    ['PT1M', 60],
    ['PT90M', 5400],
    ['PT2H', 7200],
    ['P14D', 1209600],
    ['P2W', 1209600],
    ['P1DT1H', 90000],
  ])('parses %s', (iso, seconds) => {
    expect(parseDuration(iso)).toBe(seconds);
  });

  it('rejects unsupported durations', () => {
    expect(parseDuration('P1M')).toBeUndefined();
    expect(parseDuration('1 hour')).toBeUndefined();
    expect(parseDuration('P')).toBeUndefined();
    expect(parseDuration('PT')).toBeUndefined();
  });

  it('splits into the largest unit that divides evenly', () => {
    expect(splitDuration('PT90M', ['seconds', 'minutes', 'hours', 'days'])).toEqual({ value: 90, unit: 'minutes' });
    expect(splitDuration('P14D', ['seconds', 'minutes', 'hours', 'days'])).toEqual({ value: 14, unit: 'days' });
    expect(splitDuration('PT30S', ['minutes', 'hours'])).toEqual({ value: 0.5, unit: 'minutes' });
  });

  it('formats values per unit', () => {
    expect(formatDuration(90, 'minutes')).toBe('PT90M');
    expect(formatDuration(14, 'days')).toBe('P14D');
    expect(formatDuration(5, 'seconds')).toBe('PT5S');
    expect(formatDuration(2, 'hours')).toBe('PT2H');
  });

  it('round-trips without changing the unit', () => {
    const { value, unit } = splitDuration('PT90M', ['seconds', 'minutes', 'hours', 'days']);

    expect(formatDuration(value, unit)).toBe('PT90M');
  });
});
