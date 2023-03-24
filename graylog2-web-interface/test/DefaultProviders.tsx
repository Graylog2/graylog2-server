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
import { ThemeProvider } from 'styled-components';
import { defaultTimezone, defaultUser } from 'defaultMockValues';

import CurrentUserContext from 'contexts/CurrentUserContext';
import UserDateTimeProvider from 'contexts/UserDateTimeProvider';
import { colors, utils, breakpoints, fonts, spacings } from 'theme';
import { THEME_MODE_LIGHT } from 'theme/constants';
import buttonStyles from 'components/bootstrap/styles/buttonStyles';
import aceEditorStyles from 'components/bootstrap/styles/aceEditorStyles';

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
  colors: themeColors,
  fonts,
  spacings,
  components: {
    button: buttonStyles({ colors: themeColors, utils: formattedUtils }),
    aceEditor: aceEditorStyles({ colors: themeColors }),
  },
  utils: formattedUtils,
};

type Props = {
  children: React.ReactNode,
}
const DefaultProviders = ({ children }: Props) => (
  <CurrentUserContext.Provider value={defaultUser}>
    <ThemeProvider theme={theme}>
      <UserDateTimeProvider tz={defaultTimezone}>
        {children}
      </UserDateTimeProvider>
    </ThemeProvider>
  </CurrentUserContext.Provider>
);

export default DefaultProviders;
