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
import { MantineProvider } from '@mantine/core';

import { COLOR_SCHEME_LIGHT } from 'theme/sawmill';
import SawmillSC from 'theme/sawmill/styled-components/Sawmill';
import SawmillMantine from 'theme/sawmill/mantine/Sawmill';
import CurrentUserContext from 'contexts/CurrentUserContext';
import UserDateTimeProvider from 'contexts/UserDateTimeProvider';
import DocumentTitleContextProvider from 'contexts/DocumentTitleContextProvider';

const mantineTheme = SawmillMantine({ colorScheme: COLOR_SCHEME_LIGHT });
const scTheme = SawmillSC(mantineTheme);

type Props = {
  children: React.ReactNode;
  env?: 'default' | 'test';
};

const DefaultProviders = ({ children, env = undefined }: Props) => (
  <CurrentUserContext.Provider value={defaultUser}>
    <MantineProvider theme={mantineTheme} env={env}>
      <ThemeProvider theme={{ ...scTheme, changeMode: () => {} }}>
        <UserDateTimeProvider tz={defaultTimezone}>
          <DocumentTitleContextProvider>{children}</DocumentTitleContextProvider>
        </UserDateTimeProvider>
      </ThemeProvider>
    </MantineProvider>
  </CurrentUserContext.Provider>
);

export default DefaultProviders;
