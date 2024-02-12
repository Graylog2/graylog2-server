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
import type { MantineTheme } from '@graylog/sawmill/mantine';
import SawmillMantine from '@graylog/sawmill/mantine';
import { useMemo } from 'react';
import { MantineProvider } from '@mantine/core';

import usePluginEntities from 'hooks/usePluginEntities';
import type { ThemesColors } from 'theme/theme-types';

import ColorSchemeContext from './ColorSchemeContext';
import { COLOR_SCHEMES } from './constants';
import usePreferredColorScheme from './hooks/usePreferredColorScheme';

import 'material-symbols/outlined.css';

type Props = {
  children: React.ReactNode,
  initialThemeModeOverride: ColorScheme,
  userIsLoggedIn: boolean,
}

const useSCTheme = (
  colorScheme: ColorScheme,
  changeColorScheme: (newColorScheme: ColorScheme) => void,
  useCustomThemeColors: () => ({ data: ThemesColors }),
  mantineTheme: MantineTheme,
) => {
  const { data: customThemeColors } = useCustomThemeColors?.() ?? {};

  return useMemo(() => {
    const theme = SawmillSC({
      colorScheme,
      customColors: customThemeColors?.[colorScheme],
    });

    return ({
      ...theme,
      changeMode: changeColorScheme,
      mantine: mantineTheme,
    });
  }, [changeColorScheme, colorScheme, customThemeColors, mantineTheme]);
};

const useMantineTheme = (
  colorScheme: ColorScheme,
  useCustomThemeColors: () => ({ data: ThemesColors }),
) => {
  const { data: customThemeColors } = useCustomThemeColors?.() ?? {};

  return useMemo(() => SawmillMantine({
    colorScheme,
    customColors: customThemeColors?.[colorScheme],
  }), [colorScheme, customThemeColors]);
};

const GraylogThemeProvider = ({ children, initialThemeModeOverride, userIsLoggedIn }: Props) => {
  const [colorScheme, changeColorScheme] = usePreferredColorScheme(initialThemeModeOverride, userIsLoggedIn);
  const themeCustomizer = usePluginEntities('customization.theme.customizer');
  const useCustomThemeColors = themeCustomizer?.[0]?.hooks.useCustomThemeColors;
  const mantineTheme = useMantineTheme(colorScheme, useCustomThemeColors);
  const scTheme = useSCTheme(colorScheme, changeColorScheme, useCustomThemeColors, mantineTheme);

  return (
    <ColorSchemeContext.Provider value={colorScheme}>
      <MantineProvider theme={mantineTheme}>
        <ThemeProvider theme={scTheme}>
          {children}
        </ThemeProvider>
      </MantineProvider>
    </ColorSchemeContext.Provider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.node.isRequired,
  initialThemeModeOverride: PropTypes.oneOf(COLOR_SCHEMES),
};

GraylogThemeProvider.defaultProps = {
  initialThemeModeOverride: undefined,
};

export default GraylogThemeProvider;
