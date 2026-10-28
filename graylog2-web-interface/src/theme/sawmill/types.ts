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
import { Color } from 'chroma-js';

import { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT } from './THEME_BASE';

// eslint-disable-next-line @typescript-eslint/ban-types
export type DeepPartial<T> =
  T extends Record<string, unknown>
    ? {
        [P in keyof T]?: DeepPartial<T[P]>;
      }
    : T;

export type ColorScheme = typeof COLOR_SCHEME_DARK | typeof COLOR_SCHEME_LIGHT;

export type ColorVariant = 'danger' | 'default' | 'info' | 'primary' | 'success' | 'warning' | 'gray';

export type ThemeBaseColors = {
  variant: Record<ColorVariant, [string, string, string, string, string, string, string, string, string, string]>;
  global: {
    background: string;
    contentBackground: string;
    link: string;
    navigationBoxShadow: string;
  };
  brand: {
    primary: string;
    secondary: string;
    tertiary: string;
    logo: string;
    concrete: string;
  };
  grayScale: {
    start: string;
    end: string;
  };
};

export type ThemeBase = {
  fonts: {
    families: {
      body: string;
      monospace: string;
      navigation: string;
    };
    rootSize: number;
    rootLineHeight: string;
    scale: number;
    sizes: {
      navigation: number;
      h1: number;
      h2: number;
      h3: number;
      h4: number;
      h5: number;
      h6: number;
      xs: number;
      sm: number;
      md: number;
      lg: number;
      xl: number;
      xxl: number;
    };
  };
  breakpoints: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
  };
  spacings: Array<'xxs' | 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl'>;
  colors: Record<ColorScheme, ThemeBaseColors>;
};

export type CustomColors = DeepPartial<{
  variant: Record<ColorVariant, string>;
  global: {
    background: string;
    contentBackground: string;
    link: string;
    navigationBoxShadow: string;
  };
  brand: {
    primary: string;
    secondary: string;
    tertiary: string;
    logo: string;
    concrete: string;
  };
}>;

export type Utils = {
  colorLevel: (colorHex: string, level?: number) => string;
  contrastingColor: (color: string, wcagLevel?: string) => string;
  opacify: (color: string, amount: number) => string;
  readableColor: (hex: string, darkColor?: string, lightColor?: string) => string;
  flattenColorStack: (colors: Array<string | Color>) => string;
  mixColor: (originalColor: string | Color, adjustColor: string | Color, ratio?: number) => string;
};
