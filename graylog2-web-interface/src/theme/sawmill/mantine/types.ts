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
import {
  ColorScheme,
  ColorVariant,
  CustomColors,
} from '../types';

export type MantineColors = Record<ColorVariant, [string, string, string, string, string, string, string, string, string, string]>
export type FontSizes = Record<'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl', string>;
export type Breakpoints = Record<'xs' | 'sm' | 'md' | 'lg' | 'xl', string>;
export type Spacing = Record<'xs' | 'sm' | 'md' | 'lg' | 'xl', string>;
export type Headings = {
  sizes: {
    h1: { fontSize: string };
    h2: { fontSize: string };
    h3: { fontSize: string };
    h4: { fontSize: string };
    h5: { fontSize: string };
    h6: { fontSize: string };
  };
}

export type OtherAttributes = {
  customColors: CustomColors | undefined
}

export interface MantineTheme {
  breakpoints: Breakpoints;
  colorScheme: ColorScheme,
  colors: MantineColors,
  defaultRadius: number,
  fontFamily: string,
  fontFamilyMonospace: string,
  fontSizes: FontSizes;
  headings: Headings;
  other: OtherAttributes,
  primaryColor: ColorVariant,
  primaryShade: Record<ColorScheme, 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9>,
  spacing: Spacing,
}
