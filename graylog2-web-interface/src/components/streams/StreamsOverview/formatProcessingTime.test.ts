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
import { formatProcessingTime, processingTimeSeverity } from './formatProcessingTime';

describe('formatProcessingTime', () => {
  it('returns a dash for undefined / null / negative / NaN', () => {
    expect(formatProcessingTime(undefined)).toBe('—');
    expect(formatProcessingTime(null)).toBe('—');
    expect(formatProcessingTime(-1)).toBe('—');
    expect(formatProcessingTime(Number.NaN)).toBe('—');
  });

  it('formats values < 1 second in milliseconds', () => {
    expect(formatProcessingTime(0)).toBe('0 ms');
    expect(formatProcessingTime(123)).toBe('123 ms');
    expect(formatProcessingTime(999)).toBe('999 ms');
  });

  it('formats values < 1 minute in seconds with one decimal', () => {
    expect(formatProcessingTime(1000)).toBe('1.0 s');
    expect(formatProcessingTime(3094.12)).toBe('3.1 s');
    expect(formatProcessingTime(59_999)).toBe('60.0 s');
  });

  it('formats values >= 1 minute as min + s', () => {
    expect(formatProcessingTime(60_000)).toBe('1 min 0 s');
    expect(formatProcessingTime(155_589)).toBe('2 min 36 s');
    expect(formatProcessingTime(3_600_000)).toBe('60 min 0 s');
  });
});

describe('processingTimeSeverity', () => {
  it('returns normal below the warning threshold (10 minutes)', () => {
    expect(processingTimeSeverity(0)).toBe('normal');
    expect(processingTimeSeverity(599_999)).toBe('normal');
  });

  it('returns warning at the 10-minute threshold and below the danger threshold', () => {
    expect(processingTimeSeverity(600_000)).toBe('warning');
    expect(processingTimeSeverity(3_599_999)).toBe('warning');
  });

  it('returns danger at the 60-minute threshold and above', () => {
    expect(processingTimeSeverity(3_600_000)).toBe('danger');
    expect(processingTimeSeverity(10_000_000)).toBe('danger');
  });
});
