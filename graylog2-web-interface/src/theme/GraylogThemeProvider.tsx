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
import * as React from 'react';
import { ThemeProvider } from 'styled-components';
import type { ColorScheme } from '@graylog/sawmill';
import { MantineProvider } from '@mantine/core';

import useThemes from 'theme/hooks/useThemes';

import ColorSchemeContext from './ColorSchemeContext';

import 'material-symbols/rounded.css';

type Props = {
  children: React.ReactNode,
  initialThemeModeOverride?: ColorScheme
  userIsLoggedIn: boolean,
}

const GraylogThemeProvider = ({ children, initialThemeModeOverride, userIsLoggedIn }: Props) => {
  const { scTheme, mantineTheme, colorScheme } = useThemes(initialThemeModeOverride, userIsLoggedIn);

  return (
    <ColorSchemeContext.Provider value={colorScheme}>
      <MantineProvider theme={mantineTheme}
                       forceColorScheme={colorScheme}>
        <ThemeProvider theme={scTheme}>
          {children}
        </ThemeProvider>
      </MantineProvider>
    </ColorSchemeContext.Provider>
  );
};

export default GraylogThemeProvider;
