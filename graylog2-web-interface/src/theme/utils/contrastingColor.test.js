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
import contrastingColor from './contrastingColor';

describe('contrastingColor', () => {
  it('should return a properly contrasting color', () => {
    const color1 = contrastingColor('#000');
    const color2 = contrastingColor('#fff');
    const color3 = contrastingColor('#f0f');
    const color4 = contrastingColor('#000', 'AA');

    expect(color1).toBe('rgb(151,151,151)');
    expect(color2).toBe('rgb(81,81,81)');
    expect(color3).toBe('rgb(255,249,255)');
    expect(color4).toBe('rgb(128,128,128)');
  });

  it('should accept other color strings', () => {
    const color1 = contrastingColor('rgb(0, 0, 0)');

    expect(color1).toBe('rgb(151,151,151)');
  });

  it('should accept transparent color strings', () => {
    const color1 = contrastingColor('rgba(0, 0, 0, 0.5)');

    expect(color1).toBe('rgba(151,151,151,0.675)');
  });
});
