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
import chroma from 'chroma-js';

import teint from './teint';
import {
  darken,
  lighten,
  generateGlobalColors,
  generateGrayScale,
  generateInputColors,
  generateTableColors,
  generateVariantColors,
} from './util';

const inputColor = '#f00';

describe('Theme Color Generators', () => {
  it('darkens the provided color', () => {
    const output = darken(inputColor, 0.25);

    expect(output).toBe('#dd0000');
    expect(chroma(inputColor).num() > chroma(output).num()).toBeTruthy();
  });

  it('lightens the provided color', () => {
    const output = lighten(inputColor, 0.25);

    expect(output).toBe('#ff8080');
    expect(chroma(inputColor).num() < chroma(output).num()).toBeTruthy();
  });

  it('generates global colors', () => {
    const output = generateGlobalColors('teint',
      { ...teint.brand, secondary: inputColor, tertiary: inputColor },
      { ...teint.global, contentBackground: inputColor, link: inputColor },
      { ...teint.variant, lightest: { default: inputColor } });

    expect(output).toEqual({ linkHover: '#c20000', navigationBackground: '#f00', navigationBoxShadow: 'rgba(255,0,0,0.5)', textAlt: '#f00', textDefault: '#f00' });
  });
});
