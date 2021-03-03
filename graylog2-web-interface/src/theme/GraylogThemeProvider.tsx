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
import { useState } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider, DefaultTheme } from 'styled-components';

import { breakpoints, fonts, utils } from 'theme';
import colors, { Colors } from 'theme/colors';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';
import usePluginEntities from 'views/logic/usePluginEntities';
import UpdatableThemeContext from 'theme/UpdatableThemeContext';

import useCurrentThemeMode from './UseCurrentThemeMode';

const _generateTheme = ({ changeMode, mode, generateCustomThemeColors }): DefaultTheme => {
  const currentThemeColors: Colors = generateCustomThemeColors(colors[mode], mode) ?? colors[mode];

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
    components: {
      button: buttonStyles({ colors: currentThemeColors, utils: formattedUtils }),
      aceEditor: aceEditorStyles({ colors: currentThemeColors }),
    },
    utils: formattedUtils,
  };
};

const GraylogThemeProvider = ({ children }) => {
  const [mode, changeMode] = useCurrentThemeMode();

  const themeCustomizer = usePluginEntities('customization.theme.customizer');
  const { generateCustomThemeColors } = themeCustomizer?.[0]?.actions;

  const [theme, setTheme] = useState<DefaultTheme>(_generateTheme({ changeMode, mode, generateCustomThemeColors }));

  const updatableTheme = () => {
    setTheme(_generateTheme({ changeMode, mode, generateCustomThemeColors }));
  };

  return (
    <UpdatableThemeContext.Provider value={{ updatableTheme }}>
      <ThemeProvider theme={theme}>
        {children}
      </ThemeProvider>
    </UpdatableThemeContext.Provider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default GraylogThemeProvider;
