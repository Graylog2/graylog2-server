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
import { create } from 'storybook/theming';

import SawmillMantine from 'theme/sawmill/mantine/Sawmill';
import SawmillSC from 'theme/sawmill/styled-components/Sawmill';

import { UI_ICON_COLORS } from './storybookUi';

import graylogLogoSvg from '../graylog-logo.svg?raw';

// Mirrors the pure function calls inside useThemes, without the React hook wrappers.

const baseConfiguration = {
  brandTitle: 'Graylog Design System',
  brandUrl: 'https://graylog.org/',
  brandTarget: '_block',

  appBorderRadius: 4,
  inputBorderRadius: 2,
};

// The logo is recolored with the Luma icon color for each mode (every filled shape in the SVG).
const logoFor = (colorScheme: 'light' | 'dark') => {
  const svg = graylogLogoSvg.replace(/fill="(?!none)[^"]*"/g, `fill="${UI_ICON_COLORS[colorScheme]}"`);

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
};

const colorsConfiguration = (colorScheme: 'light' | 'dark') => {
  const { colors, fonts } = SawmillSC(SawmillMantine({ colorScheme }));

  return {
    brandImage: logoFor(colorScheme),
    // Sidebar and toolbar use the page background; content areas stay on the content background.
    appBg: colors.global.background,
    appContentBg: colors.global.contentBackground,
    appPreviewBg: colors.global.contentBackground,
    appBorderColor: colors.misc.divider,
    colorPrimary: colors.brand.primary,

    textColor: colors.text.primary,
    textInverseColor: colors.global.textAlt,
    fontBase: fonts.family.body,
    fontCode: fonts.family.monospace,

    barBg: colors.global.background,
    barTextColor: colors.text.primary,

    inputBg: colors.input.background,
    inputBorder: colors.input.border,
    inputTextColor: colors.input.color,
  };
};

export const lightTheme = create({ base: 'light', ...baseConfiguration, ...colorsConfiguration('light') });
export const darkTheme = create({ base: 'dark', ...baseConfiguration, ...colorsConfiguration('dark') });
