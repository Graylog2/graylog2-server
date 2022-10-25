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
import { createGlobalStyle, css } from 'styled-components';
import * as Immutable from 'immutable';
import { Router } from 'react-router-dom';

/* eslint-disable import/no-relative-packages */
import history from '../src/util/History';
import GraylogThemeProvider from '../src/theme/GraylogThemeProvider';
import CurrentUserContext from '../src/contexts/CurrentUserContext';
import User from '../src/logic/users/User';
/* eslint-enable import/no-relative-packages */

export const adminUser = User.builder()
  .id('admin-id')
  .username('admin')
  .fullName('Administrator')
  .firstName('Administrator')
  .lastName('')
  .email('admin@example.org')
  .permissions(Immutable.List(['*']))
  .grnPermissions(Immutable.List())
  .roles(Immutable.Set(['Admin', 'Reader']))
  .readOnly(false)
  .external(true)
  .sessionActive(true)
  .sessionTimeoutMs(10000000000)
  .clientAddress('192.168.0.1')
  .accountStatus('enabled')
  .build();

const StyleGuideStyles = createGlobalStyle(({ theme }) => css`
  html {
    font-size: ${theme.fonts.size.root};
  }
`);

type Props = {
  children: React.Component,
}

const StyleGuideWrapper = ({ children }: Props) => (
  <Router history={history}>
    <CurrentUserContext.Provider value={adminUser}>
      <GraylogThemeProvider initialThemeModeOverride="teint">
        <StyleGuideStyles />
        {children}
      </GraylogThemeProvider>
    </CurrentUserContext.Provider>
  </Router>
);

export default StyleGuideWrapper;
