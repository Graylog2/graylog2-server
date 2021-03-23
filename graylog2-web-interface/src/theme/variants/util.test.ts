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
      linkHover: '#410057',
      navigationBackground: '#fff',
      navigationBoxShadow: 'rgba(243,243,243,0.5)',
      textAlt: '#fff',
      textDefault: '#1f1f1f',
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
      border: '#a6a6a6',
      borderFocus: '#7894ce',
      boxShadow: 'inset 0 1px 1px rgba(0, 0, 0, 0.075), 0 0 8px rgba(120,148,206,0.4)',
      color: '#1f1f1f',
      colorDisabled: '#9b9b9b',
      placeholder: '#9b9b9b',
    });
  });

  it('generates table colors', () => {
    const output = generateTableColors('teint', teint.variant);

    expect(output).toEqual({
      background: '#fafafa',
      backgroundAlt: '#f0f0f0',
      backgroundHover: '#f5f5f5',
      variant: {
        active: '#e6e6e6',
        danger: '#eddddd',
        info: '#dde2f0',
        primary: '#e4dee7',
        success: '#ddeddf',
        warning: '#fff5dd',
      },
      variantHover: {
        active: '#d0d0d0',
        danger: '#debdbd',
        info: '#bdc8e4',
        primary: '#cbbfd1',
        success: '#bddec2',
        warning: '#ffecbd',
      },
    });
  });

  it('generates variant colors', () => {
    const output = generateVariantColors('teint', teint.variant);

    expect(output).toEqual({
      dark: {
        danger: '#990606',
        default: '#737373',
        info: '#0057a8',
        primary: '#632275',
        success: '#009a3a',
        warning: '#e1b900',
      },
      darker: {
        danger: '#740505',
        default: '#575757',
        info: '#00427f',
        primary: '#4b1a59',
        success: '#00752c',
        warning: '#ab8d00',
      },
      darkest: {
        danger: '#3c0202',
        default: '#2d2d2d',
        info: '#002242',
        primary: '#270e2e',
        success: '#003c17',
        warning: '#584900',
      },
      light: {
        danger: '#c27878',
        default: '#a6a6a6',
        info: '#7894ce',
        primary: '#9b7ca8',
        success: '#78c385',
        warning: '#ffdd78',
      },
      lighter: {
        danger: '#debdbd',
        default: '#d0d0d0',
        info: '#bdc8e4',
        primary: '#cbbfd1',
        success: '#bddec2',
        warning: '#ffecbd',
      },
      lightest: {
        danger: '#f7efef',
        default: '#f3f3f3',
        info: '#eff2f8',
        primary: '#f2f0f4',
        success: '#eff7f0',
        warning: '#fffaef',
      },
    });
  });
});
