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
import { useCallback, createContext } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider, DefaultTheme } from 'styled-components';
import merge from 'lodash/merge';

import { breakpoints, colors, fonts, utils } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';
import AppConfig from 'util/AppConfig';

import useCurrentThemeMode from './UseCurrentThemeMode';

const customizedTheme = AppConfig.customTheme();
export const UpdateThemeContext = createContext(undefined);

const GraylogThemeProvider = ({ children }) => {
  const [mode, changeMode] = useCurrentThemeMode();

  const updateTheme = (updatedColors) => {
    console.log({ updatedColors });
  };

  const theme = useCallback((): DefaultTheme => {
    const currentTheme = merge(colors[mode], customizedTheme[mode]);

    const formattedUtils = {
      ...utils,
      colorLevel: utils.colorLevel(currentTheme),
      readableColor: utils.readableColor(currentTheme),
    };

    return {
      mode,
      changeMode,
      breakpoints,
      colors: currentTheme,
      fonts,
      components: {
        button: buttonStyles({ colors: currentTheme, utils: formattedUtils }),
        aceEditor: aceEditorStyles({ colors: currentTheme }),
      },
      utils: formattedUtils,
    };
  }, [mode, changeMode]);

  return (
    <ThemeProvider theme={theme}>
      <UpdateThemeContext.Provider value={{ updateTheme }}>
        {children}
      </UpdateThemeContext.Provider>
    </ThemeProvider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
