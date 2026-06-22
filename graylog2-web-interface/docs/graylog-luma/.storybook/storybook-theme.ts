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
import SawmillMantine from '@graylog/sawmill/mantine';
import SawmillSC from '@graylog/sawmill/styled-components';
import { create } from 'storybook/theming';

import graylogLogo from '../graylog-logo.png';

// Mirrors the pure function calls inside useThemes, without the React hook wrappers.

const baseConfiguration = {
  brandTitle: 'Graylog Design System',
  brandUrl: 'https://graylog.org/',
  brandTarget: '_block',
  brandImage: graylogLogo,

  appBorderRadius: 4,
  inputBorderRadius: 2,
};

const colorsConfiguration = (colorScheme: 'light' | 'dark') => {
  const { colors, fonts } = SawmillSC(SawmillMantine({ colorScheme }));

  return {
    appBg: colors.global.contentBackground,
    appContentBg: colors.global.contentBackground,
    appPreviewBg: colors.global.contentBackground,
    appBorderColor: colors.misc.divider,
    colorPrimary: colors.brand.primary,

    textColor: colors.text.primary,
    textInverseColor: colors.global.textAlt,
    fontBase: fonts.family.body,
    fontCode: fonts.family.monospace,

    barBg: colors.global.contentBackground,
    barTextColor: colors.text.primary,

    inputBg: colors.input.background,
    inputBorder: colors.input.border,
    inputTextColor: colors.input.color,
  };
};

export const lightTheme = create({ base: 'light', ...baseConfiguration, ...colorsConfiguration('light') });
export const darkTheme = create({ base: 'dark', ...baseConfiguration, ...colorsConfiguration('dark') });
