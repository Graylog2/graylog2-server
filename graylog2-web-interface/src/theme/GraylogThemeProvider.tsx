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
// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { breakpoints, colors, fonts, utils } from 'theme';
import type { ThemeInterface } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';

import useCurrentThemeMode from './UseCurrentThemeMode';

const GraylogThemeProvider = ({ children }) => {
  const [mode, changeMode] = useCurrentThemeMode();
  const themeColors = colors[mode];

  const theme = useCallback((): ThemeInterface => {
    const formattedUtils = {
      ...utils,
      colorLevel: utils.colorLevel(themeColors),
      readableColor: utils.readableColor(themeColors),
    };

    return {
      mode,
      changeMode,
      breakpoints,
      colors: themeColors,
      fonts,
      components: {
        button: buttonStyles({ colors: themeColors, utils: formattedUtils }),
        aceEditor: aceEditorStyles({ colors: themeColors }),
      },
      utils: formattedUtils,
    };
  }, [mode, themeColors, changeMode]);

  return (
    <ThemeProvider theme={theme}>
      {children}
    </ThemeProvider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
