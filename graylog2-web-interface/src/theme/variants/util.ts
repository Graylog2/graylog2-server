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
import { $PropertyType } from 'utility-types';

import { Colors } from 'theme/colors';

import { THEME_MODE_LIGHT, THEME_MODE_DARK, ThemeMode } from '../constants';

const lightThemeRatio = ['0.22', '0.55', '0.88'];
const darkThemeRatio = ['0.15', '0.55', '0.95'];

function lighten(color, ratio) { return chroma.mix(color, '#fff', ratio).hex(); }
function darken(color, ratio) { return chroma.mix(color, '#000', ratio).hex(); }

const generateGrayScale = (colorStart, colorEnd) => {
  const gray: $PropertyType<Colors, 'gray'> = {
    10: '',
    20: '',
    30: '',
    40: '',
    50: '',
    60: '',
    70: '',
    80: '',
    90: '',
    100: '',
  };
  const scale = chroma.scale([colorStart, colorEnd]).colors(10);

  scale.forEach((tint, index) => {
    const key = (index + 1) * 10;

    gray[key] = tint;
  });

  return gray;
};

const generateTableColors = (mode: ThemeMode, variant: $PropertyType<Colors, 'variant'>) => {
  if (![THEME_MODE_DARK, THEME_MODE_LIGHT].includes(mode)) {
    throw new Error(`Requires "${THEME_MODE_DARK}" or "${THEME_MODE_LIGHT}" mode option.`);
  }

  const adjust = mode === THEME_MODE_DARK ? darken : lighten;

  const tableColors: $PropertyType<Colors, 'table'> = {
    background: adjust(variant.default, 0.95),
    backgroundAlt: adjust(variant.default, 0.85),
    backgroundHover: adjust(variant.default, 0.9),
    variant: {
      danger: adjust(variant.danger, 0.75),
      active: adjust(variant.default, 0.75),
      info: adjust(variant.info, 0.75),
      primary: adjust(variant.primary, 0.75),
      success: adjust(variant.success, 0.75),
      warning: adjust(variant.warning, 0.75),
    },
    variantHover: {
      danger: variant.lighter.danger,
      active: variant.lighter.default,
      info: variant.lighter.info,
      primary: variant.lighter.primary,
      success: variant.lighter.success,
      warning: variant.lighter.warning,
    },
  };

  return tableColors;
};

const generateVariantColors = (mode: ThemeMode, variant) => {
  if (![THEME_MODE_DARK, THEME_MODE_LIGHT].includes(mode)) {
    throw new Error(`Requires "${THEME_MODE_DARK}" or "${THEME_MODE_LIGHT}" mode option.`);
  }

  const adjustLight = mode === THEME_MODE_DARK ? darken : lighten;
  const adjustDark = mode === THEME_MODE_DARK ? lighten : darken;
  const ratio = mode === THEME_MODE_DARK ? darkThemeRatio : lightThemeRatio;
  const variantColors = {
    lightest: { danger: '', default: '', info: '', primary: '', success: '', warning: '' },
    lighter: { danger: '', default: '', info: '', primary: '', success: '', warning: '' },
    light: { danger: '', default: '', info: '', primary: '', success: '', warning: '' },
    dark: { danger: '', default: '', info: '', primary: '', success: '', warning: '' },
    darker: { danger: '', default: '', info: '', primary: '', success: '', warning: '' },
    darkest: { danger: '', default: '', info: '', primary: '', success: '', warning: '' },
  };

  Object.keys(variant).forEach((name) => {
    if (typeof variant[name] === 'string') {
      variantColors.light[name] = adjustLight(variant[name], ratio[0]);
      variantColors.lighter[name] = adjustLight(variant[name], ratio[1]);
      variantColors.lightest[name] = adjustLight(variant[name], ratio[2]);

      variantColors.dark[name] = adjustDark(variant[name], ratio[0]);
      variantColors.darker[name] = adjustDark(variant[name], ratio[1]);
      variantColors.darkest[name] = adjustDark(variant[name], ratio[2]);
    }
  });

  return variantColors;
};

const generateInputColors = (
  mode: ThemeMode,
  global: $PropertyType<Colors, 'global'>,
  gray: $PropertyType<Colors, 'gray'>,
  variant: $PropertyType<Colors, 'variant'>,
) => {
  const input: $PropertyType<Colors, 'input'> = {
    background: global.contentBackground,
    backgroundDisabled: darken(global.contentBackground, 0.25),
    border: variant.light.default,
    borderFocus: variant.light.info,
    boxShadow: `inset 0 1px 1px rgba(0, 0, 0, 0.075), 0 0 8px ${chroma(variant.light.info).alpha(0.4).css()}`,
    color: global.textDefault,
    colorDisabled: gray[60],
    placeholder: gray[60],
  };

  return input;
};

const generateGlobalColors = (
  mode: ThemeMode,
  brand: $PropertyType<Colors, 'brand'>,
  global: $PropertyType<Colors, 'global'>,
  variant: $PropertyType<Colors, 'variant'>,
) => {
  return {
    linkHover: chroma(global.link)[mode === THEME_MODE_DARK ? 'brighten' : 'darken'](1).hex(),
    navigationBackground: global.contentBackground,
    navigationBoxShadow: chroma(variant.lightest.default).alpha(0.5).css(),
    textAlt: brand.secondary,
    textDefault: brand.tertiary,
  };
};

export {
  darken,
  lighten,
  generateGlobalColors,
  generateGrayScale,
  generateInputColors,
  generateTableColors,
  generateVariantColors,
};
