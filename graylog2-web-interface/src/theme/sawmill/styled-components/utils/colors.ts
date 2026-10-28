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

import generateColorVariants from './colorVariants';
import tableColors from './tableColors';
import generateGrayScale from './grayColors';
import inputColors from './inputColors';
import generateGlobalColors from './globalColors';
import generateButtonColors from './buttonColors';
import generatePaginationColors from './paginationColors';
import generateTextColors from './textColors';
import alertColors from './alertColors';

import { ContrastColors, DisabledColors, StyledComponentsTheme } from '../types';
import { MantineColors, MantineTheme } from '../../mantine/types';
import THEME_BASE, { COLOR_SCHEME_LIGHT, COLOR_WHITE } from '../../THEME_BASE';
import { ColorVariant } from '../../types';
import { contrastingColor, mixColor, opacify } from '../../utils';

const mixDisabledColors = (
  variant: string,
  colors: MantineColors,
  primaryShade: number,
  defaultTextColor: string,
  alternativeTextColor: string,
) => {
  const variantColor = colors[variant as ColorVariant][primaryShade];
  const buttonAdjustColor = chroma(variantColor).luminance() > 0.5 ? defaultTextColor : alternativeTextColor;
  const disabledBackground = mixColor(variantColor, buttonAdjustColor, 0.2);
  const disabledColor = contrastingColor(disabledBackground, 'AA');

  return {
    background: disabledBackground,
    color: disabledColor,
  };
};

const generateColors = (mantineTheme: MantineTheme): StyledComponentsTheme['colors'] => {
  const { colors } = mantineTheme;
  const isLightTheme = mantineTheme.colorScheme === COLOR_SCHEME_LIGHT;

  const primaryShade = mantineTheme.primaryShade[mantineTheme.colorScheme];
  const baseColors = THEME_BASE.colors[mantineTheme.colorScheme];
  const brandColors = mantineTheme.other.customColors?.brand
    ? { ...baseColors.brand, ...mantineTheme.other.customColors.brand }
    : baseColors.brand;
  const baseGlobalColors = mantineTheme.other.customColors?.global
    ? { ...baseColors.global, ...mantineTheme.other.customColors.global }
    : baseColors.global;

  const completeVariant = generateColorVariants(mantineTheme.colorScheme, colors, primaryShade);
  const completeGlobal = generateGlobalColors(mantineTheme.colorScheme, colors, brandColors, baseGlobalColors);
  const textColors = generateTextColors(isLightTheme, colors, brandColors.tertiary);

  const gray = generateGrayScale(baseColors.grayScale.start, baseColors.grayScale.end);
  const table = tableColors(mantineTheme.colorScheme, completeVariant, completeGlobal, colors);
  const input = inputColors(
    completeGlobal,
    gray,
    completeVariant,
    mantineTheme.colorScheme,
    colors,
    textColors.primary,
  );
  const disabledColors = Object.fromEntries(
    Object.keys(colors).map((variant) => [
      variant,
      mixDisabledColors(variant, colors, primaryShade, textColors.primary, completeGlobal.textAlt),
    ]),
  ) as DisabledColors;
  const contrastColors = Object.fromEntries(
    Object.keys(colors).map((variant) => [variant, contrastingColor(colors[variant as ColorVariant][primaryShade])]),
  ) as ContrastColors;
  const buttonColors = generateButtonColors(mantineTheme.colorScheme, colors, primaryShade);
  const pagination = generatePaginationColors(colors);

  return {
    brand: brandColors,
    button: buttonColors,
    contrast: contrastColors,
    disabled: disabledColors,
    section: {
      filled: {
        border: isLightTheme ? colors.gray[1] : colors.gray[9],
        background: isLightTheme ? colors.gray[0] : colors.gray[7],
      },
    },
    global: completeGlobal,
    gray,
    misc: {
      divider: isLightTheme ? colors.gray[1] : colors.gray[9],
      logoColor: isLightTheme ? colors.gray[5] : colors.gray[0],
      stripedBackground: isLightTheme ? colors.gray[0] : opacify(colors.gray[4], 0.35),
    },
    input,
    pagination,
    table,
    variant: completeVariant,

    // new
    background: {
      body: isLightTheme ? colors.gray[0] : colors.gray[8],
      content: isLightTheme ? COLOR_WHITE : colors.gray[6],
      secondaryNav: isLightTheme ? colors.gray[1] : colors.gray[7],
    },
    alerts: alertColors(colors, isLightTheme),
    badges: {
      dotBorder: isLightTheme ? colors.gray[2] : colors.gray[3],
      primary: {
        light: {
          text: isLightTheme ? colors.primary[5] : colors.primary[4],
          background: isLightTheme ? opacify(colors.primary[5], 0.1) : opacify(colors.primary[4], 0.2),
        },
        filled: {
          text: COLOR_WHITE,
          background: isLightTheme ? colors.primary[5] : colors.primary[4],
        },
      },
      danger: {
        light: {
          text: isLightTheme ? colors.danger[5] : colors.danger[4],
          background: isLightTheme ? opacify(colors.danger[5], 0.1) : opacify(colors.danger[4], 0.2),
        },
        filled: {
          text: COLOR_WHITE,
          background: isLightTheme ? colors.danger[5] : colors.danger[4],
        },
      },
      success: {
        light: {
          text: isLightTheme ? colors.success[7] : colors.success[5],
          background: opacify(colors.success[5], 0.1),
        },
        filled: {
          text: COLOR_WHITE,
          background: colors.success[5],
        },
      },
      warning: {
        light: {
          text: isLightTheme ? colors.warning[8] : colors.warning[5],
          background: opacify(colors.warning[5], 0.1),
        },
        filled: {
          text: colors.gray[9],
          background: colors.warning[5],
        },
      },
      gray: {
        light: {
          text: isLightTheme ? colors.gray[5] : colors.gray[1],
          background: isLightTheme ? opacify(colors.gray[3], 0.1) : opacify(colors.gray[3], 0.2),
        },
        filled: {
          text: COLOR_WHITE,
          background: colors.gray[3],
        },
      },
    },
    buttons: {
      disabledBackground: isLightTheme ? opacify(colors.gray[5], 0.08) : opacify(colors.gray[0], 0.1),
      blue: {
        light: {
          text: isLightTheme ? colors.primary[5] : colors.primary[4],
          background: isLightTheme ? opacify(colors.primary[5], 0.1) : opacify(colors.primary[4], 0.2),
          backgroundHover: isLightTheme ? opacify(colors.primary[5], 0.05) : opacify(colors.primary[4], 0.1),
        },
        filled: {
          text: COLOR_WHITE,
          background: isLightTheme ? colors.primary[5] : colors.primary[4],
          backgroundHover: isLightTheme ? colors.primary[4] : opacify(colors.primary[4], 0.7),
        },
        outlined: {
          text: isLightTheme ? colors.primary[5] : colors.primary[4],
          border: isLightTheme ? colors.primary[2] : opacify(colors.primary[4], 0.5),
          backgroundHover: isLightTheme ? opacify(colors.primary[5], 0.1) : opacify(colors.primary[4], 0.1),
        },
        subtle: {
          text: isLightTheme ? colors.primary[5] : colors.primary[4],
          backgroundHover: isLightTheme ? opacify(colors.primary[5], 0.2) : opacify(colors.primary[4], 0.2),
        },
      },
      red: {
        light: {
          text: isLightTheme ? colors.danger[5] : colors.danger[4],
          background: isLightTheme ? opacify(colors.danger[5], 0.1) : opacify(colors.danger[4], 0.2),
          backgroundHover: isLightTheme ? opacify(colors.danger[5], 0.05) : opacify(colors.danger[4], 0.1),
        },
        filled: {
          text: COLOR_WHITE,
          background: isLightTheme ? colors.danger[5] : colors.danger[4],
          backgroundHover: isLightTheme ? colors.danger[4] : opacify(colors.danger[4], 0.7),
        },
        outlined: {
          text: isLightTheme ? colors.danger[5] : colors.danger[4],
          border: isLightTheme ? colors.danger[2] : opacify(colors.danger[2], 0.4),
          backgroundHover: isLightTheme ? opacify(colors.danger[5], 0.1) : opacify(colors.danger[4], 0.1),
        },
        subtle: {
          text: isLightTheme ? colors.danger[5] : colors.danger[4],
          backgroundHover: isLightTheme ? opacify(colors.danger[5], 0.1) : opacify(colors.danger[4], 0.2),
        },
      },
      gray: {
        light: {
          text: isLightTheme ? colors.gray[5] : colors.gray[0],
          background: isLightTheme ? opacify(colors.gray[3], 0.1) : opacify(colors.gray[3], 0.2),
          backgroundHover: isLightTheme ? opacify(colors.gray[3], 0.05) : opacify(colors.gray[3], 0.1),
        },
        filled: {
          text: COLOR_WHITE,
          background: colors.gray[3],
          backgroundHover: isLightTheme ? colors.gray[2] : colors.gray[4],
        },
        outlined: {
          text: isLightTheme ? colors.gray[5] : colors.gray[0],
          border: isLightTheme ? colors.gray[2] : opacify(colors.gray[3], 0.6),
          backgroundHover: opacify(colors.gray[3], 0.1),
        },
        subtle: {
          text: isLightTheme ? colors.gray[5] : colors.gray[0],
          backgroundHover: isLightTheme ? opacify(colors.gray[3], 0.2) : opacify(colors.gray[3], 0.3),
        },
      },
    },
    cards: {
      background: isLightTheme ? COLOR_WHITE : colors.gray[4],
      border: isLightTheme ? colors.gray[1] : colors.gray[7],
    },
    charts: {
      containerBackground: isLightTheme ? colors.gray[0] : colors.gray[0],
      containerBorder: isLightTheme ? colors.gray[1] : colors.gray[1],
      lines: isLightTheme ? colors.gray[1] : colors.gray[4],
      highlight: colors.info[5],
      cyan: {
        markerColor: colors.info[5],
        textColor: COLOR_WHITE,
        fill: opacify(colors.info[5], 0.2),
      },
    },
    icons: {
      icon: isLightTheme ? colors.gray[4] : colors.gray[1],
      active: isLightTheme ? opacify(colors.gray[5], 0.7) : opacify(colors.gray[0], 0.7),
      disabled: isLightTheme ? opacify(colors.gray[5], 0.4) : opacify(colors.gray[0], 0.5),
    },
    link: {
      default: isLightTheme ? colors.primary[5] : colors.primary[4],
      hover: isLightTheme ? colors.primary[6] : colors.primary[3],
      disabled: isLightTheme ? opacify(colors.gray[5], 0.4) : opacify(colors.primary[0], 0.5),
    },
    menu: {
      default: {
        color: isLightTheme ? colors.gray[5] : colors.gray[0],
        hover: isLightTheme ? opacify(colors.gray[3], 0.1) : opacify(colors.gray[4], 0.2),
        activeColor: isLightTheme ? colors.primary[5] : colors.primary[4],
        activeBackground: isLightTheme ? opacify(colors.primary[5], 0.1) : opacify(colors.primary[4], 0.2),
      },
      red: {
        color: {
          color: isLightTheme ? colors.danger[5] : colors.danger[4],
          active: isLightTheme ? opacify(colors.danger[5], 0.1) : opacify(colors.danger[4], 0.2),
        },
      },
    },
    newsCards: {
      background: isLightTheme ? colors.gray[1] : colors.gray[5],
    },
    notification: {
      background: isLightTheme ? COLOR_WHITE : colors.gray[4],
      border: isLightTheme ? colors.gray[1] : colors.gray[4],
      blue: isLightTheme ? colors.primary[1] : colors.primary[4],
      red: isLightTheme ? colors.danger[1] : colors.danger[4],
      green: isLightTheme ? colors.success[1] : colors.success[4],
      progressBackground: colors.gray[5],
    },
    severity: {
      high: opacify(colors.danger[5], 0.75),
      low: opacify(colors.gray[3], 0.75),
      medium: opacify(colors.gray[5], 0.75),
      success: opacify(colors.gray[6], 0.75),
    },
    tabs: {
      hover: isLightTheme ? colors.gray[2] : colors.gray[3],
      active: isLightTheme ? colors.primary[5] : colors.primary[4],
    },
    text: textColors,
  };
};

export default generateColors;
