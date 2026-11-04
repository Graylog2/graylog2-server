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

import { useMemo } from 'react';

import type { ColorScheme } from 'theme/sawmill';
import SawmillMantine from 'theme/sawmill/mantine/Sawmill';
import type { MantineTheme } from 'theme/sawmill/mantine/Sawmill';
import SawmillSC from 'theme/sawmill/styled-components/Sawmill';
import type { CustomThemesColors } from 'theme/theme-types';
import usePreferredColorScheme from 'theme/hooks/usePreferredColorScheme';
import usePluginEntities from 'hooks/usePluginEntities';

const useMantineTheme = (colorScheme: ColorScheme, useCustomThemeColors: () => { data: CustomThemesColors }) => {
  const { data: customThemeColors } = useCustomThemeColors?.() ?? {};

  return useMemo(
    () =>
      SawmillMantine({
        colorScheme,
        customColors: customThemeColors?.[colorScheme],
      }),
    [colorScheme, customThemeColors],
  );
};

const useStyledComponentsTheme = (
  changeColorScheme: (newColorScheme: ColorScheme) => void,
  mantineTheme: MantineTheme,
) =>
  useMemo(() => {
    const theme = SawmillSC(mantineTheme);

    return {
      ...theme,
      changeMode: changeColorScheme,
      mantine: mantineTheme,
    };
  }, [changeColorScheme, mantineTheme]);

const useThemes = (initialThemeModeOverride: ColorScheme, userIsLoggedIn: boolean) => {
  const [colorScheme, changeColorScheme] = usePreferredColorScheme(initialThemeModeOverride, userIsLoggedIn);
  const themeCustomizer = usePluginEntities('customization.theme.customizer');
  const useCustomThemeColors = themeCustomizer?.[0]?.hooks.useCustomThemeColors;
  const mantineTheme = useMantineTheme(colorScheme, useCustomThemeColors);
  const scTheme = useStyledComponentsTheme(changeColorScheme, mantineTheme);

  return { scTheme, mantineTheme, colorScheme };
};

export default useThemes;
