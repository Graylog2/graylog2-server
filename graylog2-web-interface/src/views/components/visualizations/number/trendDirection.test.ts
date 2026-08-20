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
import trendDirection from './trendDirection';

describe('trendDirection', () => {
  it.each`
    current | previous | preference   | expected
    ${42}   | ${42}    | ${'NEUTRAL'} | ${'neutral'}
    ${42}   | ${42}    | ${'HIGHER'}  | ${'neutral'}
    ${42}   | ${42}    | ${'LOWER'}   | ${'neutral'}
    ${43}   | ${42}    | ${'HIGHER'}  | ${'good'}
    ${41}   | ${42}    | ${'HIGHER'}  | ${'bad'}
    ${43}   | ${42}    | ${'LOWER'}   | ${'bad'}
    ${41}   | ${42}    | ${'LOWER'}   | ${'good'}
    ${43}   | ${42}    | ${'NEUTRAL'} | ${'neutral'}
    ${41}   | ${42}    | ${'NEUTRAL'} | ${'neutral'}
  `(
    'returns $expected for current=$current, previous=$previous, preference=$preference',
    ({ current, previous, preference, expected }) => {
      expect(trendDirection(current, previous, preference)).toBe(expected);
    },
  );

  it('returns neutral when previous value is missing', () => {
    expect(trendDirection(42, undefined, 'LOWER')).toBe('neutral');
    expect(trendDirection(42, null, 'HIGHER')).toBe('neutral');
  });

  it('returns neutral when previous value is NaN', () => {
    expect(trendDirection(42, NaN, 'LOWER')).toBe('neutral');
    expect(trendDirection(42, NaN, 'HIGHER')).toBe('neutral');
  });

  it('returns neutral when current value is missing', () => {
    expect(trendDirection(undefined, 42, 'LOWER')).toBe('neutral');
  });
});
