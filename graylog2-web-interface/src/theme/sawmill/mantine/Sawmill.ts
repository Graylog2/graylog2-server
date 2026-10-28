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
import { MantineTheme } from './types';
import colorShades from './utils/colorShades';
import Theme from './generated/theme.json';
import { PRIMARY_SHADES } from './Constants';

import { CustomColors } from '../types';

const PRIMARY_COLOR = 'info';
const DEFAULT_RADIUS = 0;

const Sawmill = ({
  colorScheme,
  customColors,
}: {
  colorScheme: MantineTheme['colorScheme'],
  changeColorScheme?: (newColorScheme: MantineTheme['colorScheme']) => void,
  customColors?: CustomColors | undefined,
}): MantineTheme => ({
  defaultRadius: DEFAULT_RADIUS,
  breakpoints: Theme.breakpoints,
  colors: colorShades(colorScheme, customColors?.variant),
  colorScheme,
  fontFamily: Theme.fontFamily,
  fontFamilyMonospace: Theme.fontFamilyMonospace,
  fontSizes: Theme.fontSizes,
  headings: Theme.headings,
  other: {
    customColors,
  },
  primaryColor: PRIMARY_COLOR,
  primaryShade: PRIMARY_SHADES,
  spacing: Theme.spacing,
});

export type { MantineTheme } from './types';
export default Sawmill;
