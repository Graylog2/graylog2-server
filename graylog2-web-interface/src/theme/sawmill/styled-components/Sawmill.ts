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
import { StyledComponentsTheme } from './types';
import Theme from './generated/theme.json';
import aceEditor from './component-styles/aceEditor';
import generateColors from './utils/colors';

import {
  colorLevel, contrastingColor, opacify, readableColor, mixColor, flattenColorStack,
} from '../utils';
import { MantineTheme } from '../mantine/types';
import { COLOR_SCHEME_LIGHT } from '../THEME_BASE';

const Sawmill = (mantineTheme: MantineTheme): StyledComponentsTheme => {
  const isLightTheme = mantineTheme.colorScheme === COLOR_SCHEME_LIGHT;

  const hasCustomColors = !!mantineTheme.other.customColors && !!Object.keys(mantineTheme.other.customColors).length;
  const defaultColors = Theme.colors[mantineTheme.colorScheme];
  const colors = hasCustomColors ? generateColors(mantineTheme) : defaultColors;

  const lightTextColor = isLightTheme ? colors.global.textAlt : colors.text.primary;
  const darkTextColor = isLightTheme ? colors.text.primary : colors.global.textAlt;

  const utils = {
    colorLevel: colorLevel(colors.text.primary, colors.global.textAlt),
    readableColor: readableColor(lightTextColor, darkTextColor),
    opacify,
    contrastingColor,
    mixColor,
    flattenColorStack,
  };

  return ({
    breakpoints: Theme.breakpoints,
    components: hasCustomColors ? {
      aceEditor: aceEditor(colors),
    } : Theme.components[mantineTheme.colorScheme],
    colors,
    fonts: Theme.fonts,
    mode: mantineTheme.colorScheme,
    spacings: Theme.spacings,
    utils,
  });
};

export { generateColors };
export type { StyledComponentsTheme } from './types';
export default Sawmill;
