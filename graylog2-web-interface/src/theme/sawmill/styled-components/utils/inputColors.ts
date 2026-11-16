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

import type { StyledComponentsTheme } from '../types';
import type { ColorScheme } from '../../types';
import { COLOR_SCHEME_LIGHT } from '../../THEME_BASE';
import type { MantineColors } from '../../mantine/types';
import { opacify } from '../../utils';

const darken = (color: string, ratio: number) => chroma.mix(color, '#000', ratio).hex();

const inputColors = (
  completeGlobal: StyledComponentsTheme['colors']['global'],
  gray: StyledComponentsTheme['colors']['gray'],
  variant: StyledComponentsTheme['colors']['variant'],
  colorScheme: ColorScheme,
  colors: MantineColors,
  primaryTextColor: string,
) => {
  const background = colorScheme === COLOR_SCHEME_LIGHT ? completeGlobal.contentBackground : colors.gray[7];

  return {
    background,
    backgroundDisabled: darken(background, 0.25),
    border: colorScheme === COLOR_SCHEME_LIGHT ? colors.gray[2] : opacify(colors.default[3], 0.74),
    borderFocus: variant.light.info as string,
    boxShadow: `inset 0 1px 1px rgba(0, 0, 0, 0.075), 0 0 8px ${opacify(variant.light.info as string, 0.4)}`,
    color: primaryTextColor,
    colorDisabled: gray[60],
    placeholder: gray[60],
  };
};

export default inputColors;
