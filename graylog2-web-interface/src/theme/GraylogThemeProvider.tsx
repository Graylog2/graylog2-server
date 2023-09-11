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
import SawmillSC from '@graylog/sawmill/styled-components';
import { useMemo } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';

import ColorSchemeContext from './ColorSchemeContext';
import { COLOR_SCHEMES } from './constants';
import usePreferredColorScheme from './hooks/usePreferredColorScheme';

type Props = {
  children: React.ReactNode,
  initialThemeModeOverride: ColorScheme
}

const useGraylogTheme = (
  colorScheme: ColorScheme,
  changeColorScheme: (newColorScheme: ColorScheme) => void,
) => {
  const themeCustomizer = usePluginEntities('customization.theme.customizer');
  const useCustomThemeColors = themeCustomizer?.[0]?.hooks.useCustomThemeColors;
  const { data: customThemeColors } = useCustomThemeColors?.() ?? {};

  return useMemo(() => {
    const theme = SawmillSC({
      colorScheme,
      customColors: customThemeColors?.[colorScheme],
    });

    return ({
      ...theme,
      changeMode: changeColorScheme,
    });
  }, [changeColorScheme, colorScheme, customThemeColors]);
};

const GraylogThemeProvider = ({ children, initialThemeModeOverride }: Props) => {
  const [colorScheme, changeColorScheme] = usePreferredColorScheme(initialThemeModeOverride);
  const theme = useGraylogTheme(colorScheme, changeColorScheme);

  return theme ? (
    <ColorSchemeContext.Provider value={colorScheme}>
      <ThemeProvider theme={theme}>
        {children}
      </ThemeProvider>
    </ColorSchemeContext.Provider>
  ) : null;
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.node.isRequired,
  initialThemeModeOverride: PropTypes.oneOf(COLOR_SCHEMES),
};

GraylogThemeProvider.defaultProps = {
  initialThemeModeOverride: undefined,
};

export default GraylogThemeProvider;
