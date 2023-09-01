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
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';
import type { ColorScheme } from '@graylog/sawmill';
import { SawmillSC } from '@graylog/sawmill';
import { useMemo } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';

import RegeneratableThemeContext from './RegeneratableThemeContext';
import ThemeModeContext from './ThemeModeContext';
import { THEME_MODES, TO_LEGACY_THEME_MODE } from './constants';
import useCurrentThemeMode from './UseCurrentThemeMode';

type Props = {
  children: React.ReactNode,
  initialThemeModeOverride: ColorScheme
}

const GraylogThemeProvider = ({ children, initialThemeModeOverride }: Props) => {
  const [colorScheme, changeColorScheme] = useCurrentThemeMode(initialThemeModeOverride);
  const themeCustomizer = usePluginEntities('customization.theme.customizer');
  const { customThemeColors } = themeCustomizer?.[0]?.hooks?.useThemeColors() ?? {};

  const theme = useMemo(() => new SawmillSC({
    colorScheme,
    changeColorScheme,
    customColors: customThemeColors?.[TO_LEGACY_THEME_MODE[colorScheme]],
  }), [changeColorScheme, colorScheme, customThemeColors]);

  return theme ? (
    <RegeneratableThemeContext.Provider value={theme}>
      <ThemeModeContext.Provider value={colorScheme}>
        <ThemeProvider theme={theme}>
          {children}
        </ThemeProvider>
      </ThemeModeContext.Provider>
    </RegeneratableThemeContext.Provider>
  ) : null;
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.node.isRequired,
  initialThemeModeOverride: PropTypes.oneOf(THEME_MODES),
};

GraylogThemeProvider.defaultProps = {
  initialThemeModeOverride: undefined,
};

export default GraylogThemeProvider;
