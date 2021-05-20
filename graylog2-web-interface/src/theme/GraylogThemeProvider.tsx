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
import { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider, DefaultTheme } from 'styled-components';

import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';
import usePluginEntities from 'views/logic/usePluginEntities';

import { breakpoints, colors, fonts, utils, spacing } from './index';
import RegeneratableThemeContext from './RegeneratableThemeContext';
import { Colors } from './colors';
import { THEME_MODES, ThemeMode } from './constants';
import useCurrentThemeMode from './UseCurrentThemeMode';

interface generateCustomThemeColorsType {
  graylogColors: Colors,
  mode: ThemeMode,
  initialLoad: boolean,
}

interface generateThemeType {
  changeMode: (ThemeMode) => void,
  mode: ThemeMode,
  initialLoad?: boolean,
  generateCustomThemeColors: ({ graylogColors, mode, initialLoad }: generateCustomThemeColorsType) => Promise<Colors> | undefined,
}

function buildTheme(currentThemeColors, changeMode, mode): DefaultTheme {
  const formattedUtils = {
    ...utils,
    colorLevel: utils.colorLevel(currentThemeColors),
    readableColor: utils.readableColor(currentThemeColors),
  };

  return {
    mode,
    changeMode,
    breakpoints,
    colors: currentThemeColors,
    fonts,
    spacing,
    components: {
      button: buttonStyles({ colors: currentThemeColors, utils: formattedUtils }),
      aceEditor: aceEditorStyles({ colors: currentThemeColors }),
    },
    utils: formattedUtils,
  };
}

const _generateTheme = ({ changeMode, mode, generateCustomThemeColors, initialLoad = false }: generateThemeType) => {
  if (generateCustomThemeColors) {
    return generateCustomThemeColors({
      graylogColors: colors[mode],
      mode,
      initialLoad,
    }).then((currentThemeColors) => {
      return buildTheme(currentThemeColors, changeMode, mode);
    });
  }

  return Promise.resolve(colors[mode]).then((currentThemeColors) => {
    return buildTheme(currentThemeColors, changeMode, mode);
  });
};

const GraylogThemeProvider = ({ children, initialThemeModeOverride }) => {
  const [mode, changeMode] = useCurrentThemeMode(initialThemeModeOverride);

  const themeCustomizer = usePluginEntities('customization.theme.customizer');
  const generateCustomThemeColors = themeCustomizer?.[0]?.actions?.generateCustomThemeColors;

  const [theme, setTheme] = useState<DefaultTheme>();

  useEffect(() => {
    _generateTheme({ changeMode, mode, generateCustomThemeColors, initialLoad: true }).then(setTheme);
  }, [changeMode, generateCustomThemeColors, mode, setTheme]);

  const regenerateTheme = () => {
    _generateTheme({ changeMode, mode, generateCustomThemeColors, initialLoad: false }).then(setTheme);
  };

  return theme ? (
    <RegeneratableThemeContext.Provider value={{ regenerateTheme }}>
      <ThemeProvider theme={theme}>
        {children}
      </ThemeProvider>
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
