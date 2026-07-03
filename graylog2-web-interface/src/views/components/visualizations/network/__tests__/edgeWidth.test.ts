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
import edgeWidth from '../edgeWidth';

describe('edgeWidth', () => {
  it('maps the minimum value to the minimum width', () => {
    expect(edgeWidth(2, 2, 10)).toBe(1);
  });

  it('maps the maximum value to the maximum width', () => {
    expect(edgeWidth(10, 2, 10)).toBe(8);
  });

  it('maps the midpoint value to the midpoint width', () => {
    expect(edgeWidth(6, 2, 10)).toBe(4.5);
  });

  it('returns the minimum width when all values are equal', () => {
    expect(edgeWidth(5, 5, 5)).toBe(1);
  });

  it('handles negative values by mapping the minimum to the thinnest width', () => {
    expect(edgeWidth(-10, -10, 10)).toBe(1);
    expect(edgeWidth(10, -10, 10)).toBe(8);
  });
});
