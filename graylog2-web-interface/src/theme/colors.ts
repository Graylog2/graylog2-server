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
import PropTypes from 'prop-types';

import teint from './variants/teint';
import noir from './variants/noir';

export type Colors = {
  brand: {
    primary: string,
    secondary: string,
    tertiary: string,
  },
  global: {
    background: string,
    contentBackground: string,
    link: string,
    linkHover: string,
    navigationBackground: string,
    navigationBoxShadow: string,
    textAlt: string,
    textDefault: string,
  },
  gray: {
    '10': string,
    '20': string,
    '30': string,
    '40': string,
    '50': string,
    '60': string,
    '70': string,
    '80': string,
    '90': string,
    '100': string,
  },
  input: {
    background: string,
    backgroundDisabled: string,
    border: string,
    borderFocus: string,
    boxShadow: string,
    color: string,
    colorDisabled: string,
    placeholder: string,
  },
  table: {
    background: string,
    backgroundAlt: string,
    backgroundHover: string,
    variant: {
      active: string,
      danger: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
    variantHover: {
      active: string,
      danger: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
  },
  variant: {
    danger: string,
    dark: {
      danger: string,
      default: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
    darker: {
      danger: string,
      default: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
    darkest: {
      danger: string,
      default: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
    default: string,
    info: string,
    light: {
      danger: string,
      default: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
    lighter: {
      danger: string,
      default: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
    lightest: {
      danger: string,
      default: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
    primary: string,
    success: string,
    warning: string,
  },
};

export type ColorVariants = 'danger' | 'default' | 'info' | 'primary' | 'success' | 'warning';

export const colorsPropTypes = PropTypes.shape({
  brand: PropTypes.shape({
    primary: PropTypes.string,
    secondary: PropTypes.string,
    tertiary: PropTypes.string,
  }),
  global: PropTypes.shape({
    background: PropTypes.string,
    contentBackground: PropTypes.string,
    inputBackground: PropTypes.string,
    link: PropTypes.string,
    linkHover: PropTypes.string,
    navigationBackground: PropTypes.string,
    navigationBoxShadow: PropTypes.string,
    textAlt: PropTypes.string,
    textDefault: PropTypes.string,
  }),
  gray: PropTypes.shape({
    10: PropTypes.string,
    20: PropTypes.string,
    30: PropTypes.string,
    40: PropTypes.string,
    50: PropTypes.string,
    60: PropTypes.string,
    70: PropTypes.string,
    80: PropTypes.string,
    90: PropTypes.string,
    100: PropTypes.string,
  }),
  input: PropTypes.shape({
    background: PropTypes.string,
    backgroundDisabled: PropTypes.string,
    border: PropTypes.string,
    borderFocus: PropTypes.string,
    boxShadow: PropTypes.string,
    color: PropTypes.string,
    colorDisabled: PropTypes.string,
  }),
  table: PropTypes.shape({
    background: PropTypes.string,
    backgroundAlt: PropTypes.string,
    backgroundHover: PropTypes.string,
    variant: PropTypes.shape({
      active: PropTypes.string,
      danger: PropTypes.string,
      info: PropTypes.string,
      primary: PropTypes.string,
      success: PropTypes.string,
      warning: PropTypes.string,
    }),
    variantHover: PropTypes.shape({
      active: PropTypes.string,
      danger: PropTypes.string,
      info: PropTypes.string,
      primary: PropTypes.string,
      success: PropTypes.string,
      warning: PropTypes.string,
    }),
  }),
  variant: PropTypes.shape({
    danger: PropTypes.string,
    dark: PropTypes.shape({
      danger: PropTypes.string,
      default: PropTypes.string,
      info: PropTypes.string,
      primary: PropTypes.string,
      success: PropTypes.string,
      warning: PropTypes.string,
    }),
    darker: PropTypes.shape({
      danger: PropTypes.string,
      default: PropTypes.string,
      info: PropTypes.string,
      primary: PropTypes.string,
      success: PropTypes.string,
      warning: PropTypes.string,
    }),
    darkest: PropTypes.shape({
      danger: PropTypes.string,
      default: PropTypes.string,
      info: PropTypes.string,
      primary: PropTypes.string,
      success: PropTypes.string,
      warning: PropTypes.string,
    }),
    default: PropTypes.string,
    info: PropTypes.string,
    light: PropTypes.shape({
      danger: PropTypes.string,
      default: PropTypes.string,
      info: PropTypes.string,
      primary: PropTypes.string,
      success: PropTypes.string,
      warning: PropTypes.string,
    }),
    lighter: PropTypes.shape({
      danger: PropTypes.string,
      default: PropTypes.string,
      info: PropTypes.string,
      primary: PropTypes.string,
      success: PropTypes.string,
      warning: PropTypes.string,
    }),
    lightest: PropTypes.shape({
      danger: PropTypes.string,
      default: PropTypes.string,
      info: PropTypes.string,
      primary: PropTypes.string,
      success: PropTypes.string,
      warning: PropTypes.string,
    }),
    primary: PropTypes.string,
    success: PropTypes.string,
    warning: PropTypes.string,
  }),
});

export type ThemeColorModes = {
  teint: Colors,
  noir: Colors,
};

const colors: ThemeColorModes = {
  teint,
  noir,
};

export default colors;
export {
  noir,
  teint,
};
