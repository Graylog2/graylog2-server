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
import React from 'react';
import 'theme/theme-styles';
import type { Decorator } from '@storybook/react';
import { useDarkMode } from 'storybook-dark-mode';
import { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT } from '@graylog/sawmill';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import GlobalThemeStyles from 'theme/GlobalThemeStyles';
import Notifications from 'routing/Notifications';

const GraylogThemeDecorator = ({ children }: { children: React.ReactNode }) => {
  const colorScheme = useDarkMode() ? COLOR_SCHEME_DARK : COLOR_SCHEME_LIGHT;

  return (
    <GraylogThemeProvider initialThemeModeOverride={colorScheme} key={colorScheme} userIsLoggedIn={true}>
      <GlobalThemeStyles />
      <Notifications />
      {children}
    </GraylogThemeProvider>
  );
};

export const withGraylogTheme: Decorator = (Story) => (
  <GraylogThemeDecorator>
    <Story />
  </GraylogThemeDecorator>
);
