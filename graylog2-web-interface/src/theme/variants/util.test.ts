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
    const output = generateGlobalColors('teint', teint.brand, teint.global, teint.variant);

    expect(output).toEqual({
      linkHover: '#1a609b',
      navigationBackground: '#fff',
      navigationBoxShadow: 'rgba(245,246,248,0.5)',
      textAlt: '#fff',
      textDefault: '#3e434c',
    });
  });

  it('generates gray scale', () => {
    const output = generateGrayScale('#000', '#fff');

    expect(output).toEqual({
      10: '#000000',
      20: '#1c1c1c',
      30: '#393939',
      40: '#555555',
      50: '#717171',
      60: '#8e8e8e',
      70: '#aaaaaa',
      80: '#c6c6c6',
      90: '#e3e3e3',
      100: '#ffffff',
    });
  });

  it('generates input colors', () => {
    const output = generateInputColors('teint', teint.global, teint.gray, teint.variant);

    expect(output).toEqual({
      background: '#fff',
      backgroundDisabled: '#dddddd',
      border: '#b5bfcd',
      borderFocus: '#8eadd8',
      boxShadow: 'inset 0 1px 1px rgba(0, 0, 0, 0.075), 0 0 8px rgba(142,173,216,0.4)',
      color: '#3e434c',
      colorDisabled: '#a9abaf',
      placeholder: '#a9abaf',
    });
  });

  it('generates table colors', () => {
    const output = generateTableColors('teint', teint.variant);

    expect(output).toMatchSnapshot();
  });

  it('generates variant colors', () => {
    const output = generateVariantColors('teint', teint.variant);

    expect(output).toMatchSnapshot();
  });
});
