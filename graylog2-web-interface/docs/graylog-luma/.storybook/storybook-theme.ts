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
const { colors: light, fonts } = SawmillSC(SawmillMantine({ colorScheme: 'light' }));
const { colors: dark } = SawmillSC(SawmillMantine({ colorScheme: 'dark' }));

const accentColor = light.variant.info;

const baseTheme = {
  brandTitle: 'Graylog Design System',
  brandUrl: 'https://graylog.org/',
  brandTarget: '_block',
  brandImage: graylogLogo,

  fontBase: fonts.family.body,
  fontCode: fonts.family.monospace,

  colorPrimary: accentColor,
  colorSecondary: '#9A6BFE',

  appBorderRadius: 4,
  inputBorderRadius: 2,
};

export const lightTheme = create({
  base: 'light',
  ...baseTheme,
  appBg: light.background.body,
  appContentBg: light.background.content,
  appPreviewBg: light.background.body,
  appBorderColor: light.misc.divider,

  textColor: light.text.primary,
  textInverseColor: light.global.textAlt,

  barTextColor: light.text.primary,
  barSelectedColor: accentColor,
  barHoverColor: accentColor,
  barBg: light.background.body,

  inputBg: light.background.body,
  inputBorder: light.input.border,
  inputTextColor: light.input.color,
});

export const darkTheme = create({
  base: 'dark',
  ...baseTheme,
  appBg: dark.global.background,
  appContentBg: dark.background.content,
  appPreviewBg: dark.global.background,
  appBorderColor: dark.background.secondaryNav,

  textColor: dark.text.primary,
  textInverseColor: dark.global.textAlt,

  barTextColor: dark.text.primary,
  barSelectedColor: accentColor,
  barHoverColor: accentColor,
  barBg: dark.global.background,

  inputBg: dark.input.background,
  inputBorder: dark.input.border,
  inputTextColor: dark.input.color,
});
