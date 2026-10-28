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
import breakpoints from './breakpoints';

import THEME_BASE, { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT } from '../../THEME_BASE';
import spacings from '../../styled-components/utils/spacings';
import colors from '../../styled-components/utils/colors';
import aceEditor from '../../styled-components/component-styles/aceEditor';
import fontSize from '../../utils/fontSize';
import MantineSawmill from '../../mantine/Sawmill';

const buildStyledComponentsTheme = () => {
  const lightMantineTheme = MantineSawmill({ colorScheme: COLOR_SCHEME_LIGHT });
  const darkMantineTheme = MantineSawmill({ colorScheme: COLOR_SCHEME_DARK });

  const lightColors = colors(lightMantineTheme);
  const darkColors = colors(darkMantineTheme);

  return ({
    colors: {
      [COLOR_SCHEME_LIGHT]: lightColors,
      [COLOR_SCHEME_DARK]: darkColors,
    },
    fonts: {
      family: {
        body: THEME_BASE.fonts.families.body,
        monospace: THEME_BASE.fonts.families.monospace,
        navigation: THEME_BASE.fonts.families.navigation,
      },
      lineHeight: { body: THEME_BASE.fonts.rootLineHeight },
      size: {
        root: `${THEME_BASE.fonts.rootSize}px`,
        body: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.md),
        huge: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.xxl),
        large: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.lg),
        extraLarge: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.xl),
        small: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.sm),
        tiny: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.xs),
        navigation: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.navigation),
        h1: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.h1),
        h2: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.h2),
        h3: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.h3),
        h4: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.h4),
        h5: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.h5),
        h6: fontSize(THEME_BASE.fonts.scale, THEME_BASE.fonts.sizes.h6),
      },
    },
    spacings: spacings(THEME_BASE.spacings, THEME_BASE.fonts.rootSize),
    breakpoints: breakpoints(THEME_BASE.breakpoints),
    components: {
      [COLOR_SCHEME_LIGHT]: {
        aceEditor: aceEditor(lightColors),
      },
      [COLOR_SCHEME_DARK]: {
        aceEditor: aceEditor(darkColors),
      },
    },
  });
};

export default buildStyledComponentsTheme;
