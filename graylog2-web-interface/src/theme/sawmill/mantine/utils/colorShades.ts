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
import { ColorScheme, CustomColors } from '../../types';
import { MantineColors } from '../types';
import { darken, lighten } from '../../utils/colors';
import THEME_BASE from '../../THEME_BASE';

const colorShades = (colorScheme: ColorScheme, customBaseVariantColors?: CustomColors['variant']): MantineColors => {
  const defaultVariantColors = THEME_BASE.colors[colorScheme].variant;

  if (!customBaseVariantColors || !Object.keys(customBaseVariantColors).length) {
    return defaultVariantColors;
  }

  const customVariantColors = Object.fromEntries(
    Object.entries(customBaseVariantColors).map(([variantName, color]) => [
      variantName,
      [
        lighten(color, 0.85),
        lighten(color, 0.73),
        lighten(color, 0.56),
        lighten(color, 0.35),
        lighten(color, 0.14),
        color,
        darken(color, 0.1),
        darken(color, 0.28),
        darken(color, 0.45),
        darken(color, 0.66),
      ],
    ]),
  ) as MantineColors;

  return { ...defaultVariantColors, ...customVariantColors };
};

export default colorShades;
