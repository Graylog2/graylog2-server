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
import type { $PropertyType } from 'utility-types';

import {
  generateTableColors,
  generateGrayScale,
  generateGlobalColors,
  generateInputColors,
  generateVariantColors,
} from './util';

import type { Colors } from '../colors';
import { THEME_MODE_LIGHT } from '../constants';

const brand: $PropertyType<Colors, 'brand'> = {
  primary: '#ff3633',
  secondary: '#fff',
  tertiary: '#3e434c',
  logo: '#6C7585',
};

const globalDefault: $PropertyType<Colors, 'global'> = {
  background: '#eeeff2',
  contentBackground: '#fff',
  link: '#578dcc',
  textAlt: '',
  textDefault: '',
  linkHover: '',
  navigationBackground: '',
  navigationBoxShadow: '',
};

const variantDefault = {
  danger: '#eb5454',
  default: '#9aa8bd',
  info: '#578dcc',
  primary: '#697586',
  success: '#7eb356',
  warning: '#eedf64',
};

const variant: $PropertyType<Colors, 'variant'> = {
  ...variantDefault,
  ...generateVariantColors(THEME_MODE_LIGHT, variantDefault),
};

const global = {
  ...globalDefault,
  ...generateGlobalColors(THEME_MODE_LIGHT, brand, globalDefault, variant),
};

const gray: $PropertyType<Colors, 'gray'> = generateGrayScale(brand.tertiary, brand.secondary);
const table: $PropertyType<Colors, 'table'> = generateTableColors(THEME_MODE_LIGHT, variant);
const input: $PropertyType<Colors, 'input'> = generateInputColors(THEME_MODE_LIGHT, global, gray, variant);

const teint: Colors = {
  brand,
  global,
  gray,
  input,
  table,
  variant,
};

export default teint;
