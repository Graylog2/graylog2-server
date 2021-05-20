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
import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';
import { Router } from 'react-router-dom';

import history from 'util/History';
import { breakpoints, colors, fonts, utils, margins } from 'theme';
import { THEME_MODE_LIGHT } from 'theme/constants';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';

const WrappingContainer = ({ children }) => {
  const themeColors = colors[THEME_MODE_LIGHT];
  const formattedUtils = {
    ...utils,
    colorLevel: utils.colorLevel(themeColors),
    readableColor: utils.readableColor(themeColors),
  };

  const theme = {
    mode: THEME_MODE_LIGHT,
    changeMode: () => {},
    breakpoints,
    margins,
    colors: themeColors,
    fonts,
    components: {
      button: buttonStyles({ colors: themeColors, utils: formattedUtils }),
      aceEditor: aceEditorStyles({ colors: themeColors }),
    },
    utils: formattedUtils,
  };

  return (
    <Router history={history}>
      <ThemeProvider theme={theme}>
        {children}
      </ThemeProvider>
    </Router>
  );
};

WrappingContainer.propTypes = {
  children: PropTypes.node.isRequired,
};

export default WrappingContainer;
