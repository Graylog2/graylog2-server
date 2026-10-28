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
import { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT, VARIANT_NAMES } from '../../THEME_BASE';
import { ColorScheme, ColorVariant } from '../../types';
import { MantineColors } from '../../mantine/types';
import opacify from '../../utils/opacify';
import { contrastingColor } from '../../utils';
import { ButtonColors } from '../types';

const buttonColors = (colorScheme: ColorScheme, colors: MantineColors, primaryShade: number) => {
  const customColors: Partial<Record<ColorVariant, Partial<Record<ColorScheme, string>>>> = {
    gray: {
      [COLOR_SCHEME_LIGHT]: opacify(colors.gray[2], 0.4),
      [COLOR_SCHEME_DARK]: colors.gray[3],
    },
  };

  return Object.fromEntries(
    VARIANT_NAMES.map((variantName) => {
      const background = customColors[variantName]?.[colorScheme] ?? colors[variantName][primaryShade];

      return [variantName, { background, color: contrastingColor(background) }];
    }),
  ) as ButtonColors;
};

export default buttonColors;
