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
import normalizeEdgeValue from '../normalizeEdgeValue';

describe('normalizeEdgeValue', () => {
  it('maps the minimum value to 0', () => {
    expect(normalizeEdgeValue(2, 2, 10)).toBe(0);
  });

  it('maps the maximum value to 1', () => {
    expect(normalizeEdgeValue(10, 2, 10)).toBe(1);
  });

  it('maps the midpoint value to 0.5', () => {
    expect(normalizeEdgeValue(6, 2, 10)).toBe(0.5);
  });

  it('returns 0 when all values are equal', () => {
    expect(normalizeEdgeValue(5, 5, 5)).toBe(0);
  });

  it('handles negative ranges', () => {
    expect(normalizeEdgeValue(-10, -10, 10)).toBe(0);
    expect(normalizeEdgeValue(0, -10, 10)).toBe(0.5);
    expect(normalizeEdgeValue(10, -10, 10)).toBe(1);
  });
});
