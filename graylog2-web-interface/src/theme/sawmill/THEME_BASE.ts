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
import type { ColorVariant, ThemeBase } from './types';

export const COLOR_SCHEME_DARK = 'dark';
export const COLOR_SCHEME_LIGHT = 'light';

export const VARIANT_NAMES: Array<ColorVariant> = [
  'danger',
  'default',
  'info',
  'primary',
  'success',
  'warning',
  'gray',
];

const colorVariants: Record<string, [string, string, string, string, string, string, string, string, string, string]> =
  {
    danger: [
      '#FFEDED',
      '#FFC7C7',
      '#FFACAB',
      '#FE8685',
      '#FE6E6D',
      '#FE4A49',
      '#E74342',
      '#B43534',
      '#8C2928',
      '#6B1F1F',
    ],
    default: [
      '#FAF9FF',
      '#EEEDF6',
      '#CAC8D7',
      '#8D8AA9',
      '#362F62',
      '#20194D',
      '#1A143E',
      '#130F2E',
      '#0D0A1F',
      '#06050F',
    ],
    info: [
      '#EBFAFF',
      '#C2F0FF',
      '#85E0FF',
      '#47D1FF',
      '#1FC7FF',
      '#03C2FF',
      '#00A8E0',
      '#008AB8',
      '#006B8F',
      '#003D52',
    ],
    primary: [
      '#D9EBFF',
      '#ADD0FF',
      '#7FB8FF',
      '#65A9FF',
      '#4396FF',
      '#1877EF',
      '#1064CD',
      '#0C50A5',
      '#083772',
      '#032551',
    ],
    success: [
      '#E1FFF4',
      '#ACFFE0',
      '#69F9C3',
      '#62F3BC',
      '#45E5A8',
      '#2ECA8F',
      '#22B07A',
      '#1A9869',
      '#128057',
      '#094F35',
    ],
    warning: [
      '#FFFBEB',
      '#FFF2C2',
      '#FFEDAD',
      '#FFE485',
      '#FFDC5C',
      '#FFCA0A',
      '#FFCA0A',
      '#CCA000',
      '#7A6000',
      '#292000',
    ],
    gray: [
      '#F6F7FC',
      '#E1E4ED',
      '#C1C7DC',
      '#6F768F',
      '#394261',
      '#252D47',
      '#232B43',
      '#1C2235',
      '#12182B',
      '#060C1F',
    ],
  };

export const COLOR_WHITE = '#FFFFFF';

// The default themes are generated from this file. Run `yarn generate:themes` after changing it.
const THEME_BASE: ThemeBase = {
  fonts: {
    families: {
      body: '"Source Sans Pro", "Helvetica Neue", Helvetica, Arial, sans-serif',
      monospace: '"Ubuntu Mono", Menlo, Monaco, Consolas, "Courier New", monospace',
      navigation: '"DM Sans", "Helvetica Neue", Helvetica, Arial, sans-serif',
    },
    // Scale, the root font size and the font power will be used to calculate font sizes.
    /* Scaled 1.067 Minor Second - https://type-scale.com/ */
    rootSize: 16,
    rootLineHeight: '1.24px',
    scale: 1.067,
    sizes: {
      navigation: -1,
      h1: 7,
      h2: 4,
      h3: 3,
      h4: 2,
      h5: 1,
      h6: 0,
      xs: -3,
      sm: -2,
      md: 0,
      lg: 1,
      xl: 5,
      xxl: 14,
    },
  },
  breakpoints: {
    xs: 480,
    sm: 768,
    md: 1024,
    lg: 1184,
    xl: 1440,
  },
  spacings: ['xxs', 'xs', 'sm', 'md', 'lg', 'xl', 'xxl'],
  colors: {
    [COLOR_SCHEME_LIGHT]: {
      variant: colorVariants,
      global: {
        background: '#EEEDF6',
        contentBackground: '#fff',
        link: '#1877EF',
        navigationBoxShadow: 'rgba(245,246,248,0.5)',
      },
      brand: {
        primary: '#ff3633',
        secondary: '#fff',
        tertiary: '#252D47',
        logo: '#03C2FF',
        concrete: '#697586',
      },
      grayScale: {
        start: '#252D47',
        end: '#fff',
      },
    },
    [COLOR_SCHEME_DARK]: {
      variant: colorVariants,
      global: {
        background: '#12182B',
        contentBackground: '#232B43',
        link: '#4396FF',
        navigationBoxShadow: 'rgba(20,20,20,0.5)',
      },
      brand: {
        primary: '#ff3633',
        secondary: '#252D47',
        tertiary: '#F6F7FC',
        logo: '#fff',
        concrete: '#697586',
      },
      grayScale: {
        start: '#fff',
        end: '#888',
      },
    },
  },
};

export default THEME_BASE;
