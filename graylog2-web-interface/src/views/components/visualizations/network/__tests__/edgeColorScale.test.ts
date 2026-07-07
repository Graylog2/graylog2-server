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
import edgeColorScale from '../edgeColorScale';

describe('edgeColorScale', () => {
  it("follows Plotly's domain order for YlOrRd (0 = dark red, 1 = pale yellow)", () => {
    const scale = edgeColorScale('YlOrRd');

    // Matches Plotly's YlOrRd definition, so edges map values the same way the nodes do.
    expect(scale(0).hex()).toBe('#800026');
    expect(scale(1).hex()).toBe('#ffffcc');
  });

  it('interpolates between stops for intermediate positions', () => {
    const scale = edgeColorScale('YlOrRd');
    const mid = scale(0.5).hex();

    expect(mid).toMatch(/^#[0-9a-f]{6}$/i);
    expect(mid).not.toBe(scale(0).hex());
    expect(mid).not.toBe(scale(1).hex());
  });

  it('supports Plotly-native scales such as Viridis', () => {
    const scale = edgeColorScale('Viridis');

    expect(scale(0).hex()).toMatch(/^#[0-9a-f]{6}$/i);
    expect(scale(1).hex()).toMatch(/^#[0-9a-f]{6}$/i);
    expect(scale(0).hex()).not.toBe(scale(1).hex());
  });
});
