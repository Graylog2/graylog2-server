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

import GraylogThemeProvider from 'theme/GraylogThemeProvider';

import CurrentUserPreferencesProvider from './CurrentUserPreferencesProvider';
import CurrentUserProvider from './CurrentUserProvider';
import UserDateTimeProvider from './UserDateTimeProvider';

import GlobalThemeStyles from '../theme/GlobalThemeStyles';

const ThemeAndUserProvider = ({ children }) => (
  <CurrentUserProvider>
    <UserDateTimeProvider>
      <CurrentUserPreferencesProvider>
        <GraylogThemeProvider>
          <GlobalThemeStyles />
          {children}
        </GraylogThemeProvider>
      </CurrentUserPreferencesProvider>
    </UserDateTimeProvider>
  </CurrentUserProvider>
);

ThemeAndUserProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default ThemeAndUserProvider;
