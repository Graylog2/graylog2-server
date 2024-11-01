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
import { createRoot } from 'react-dom/client';
import { Notifications } from '@mantine/notifications';

import PreflightThemeProvider from 'preflight/theme/PreflightThemeProvider';
import GlobalThemeStyles from 'preflight/theme/GlobalThemeStyles';
import DefaultQueryClientProvider from 'contexts/DefaultQueryClientProvider';
import ThemeWrapper from 'preflight/theme/ThemeWrapper';

import App from './App';

import '@mantine/core/styles.css';
import '@mantine/notifications/styles.css';
import '@mantine/dropzone/styles.css';

const appContainer = document.querySelector('div#app-root');
const root = createRoot(appContainer);

root.render((
  <PreflightThemeProvider>
    <GlobalThemeStyles />
    <DefaultQueryClientProvider>
      <ThemeWrapper>
        <>
          <Notifications />
          <App />
        </>
      </ThemeWrapper>
    </DefaultQueryClientProvider>
  </PreflightThemeProvider>
));
