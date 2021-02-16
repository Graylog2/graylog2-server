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
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider, DefaultTheme } from 'styled-components';
import merge from 'lodash/merge';

import { breakpoints, colors, fonts, utils } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';
import AppConfig from 'util/AppConfig';
import UpdatableThemeContext from 'theme/UpdatableThemeContext';

import useCurrentThemeMode from './UseCurrentThemeMode';

const customizedTheme = AppConfig.customTheme();

const GraylogThemeProvider = ({ children }) => {
  const [mode, changeMode] = useCurrentThemeMode();

  const updatableTheme = (newColors: any) => {
    console.log(newColors);
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
