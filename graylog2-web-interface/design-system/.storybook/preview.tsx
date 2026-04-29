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
import type { Decorator, Preview } from '@storybook/react';
import { MemoryRouter } from 'react-router-dom';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';

import '@graylog/sawmill/fonts';
import '@mantine/core/styles.css';
import '@mantine/dropzone/styles.css';
import '@mantine/notifications/styles.css';

const GlobalStyles = createGlobalStyle(
  ({ theme }) => css`
    html {
      font-size: ${theme.fonts.size.root};
    }
  `,
);

const withGraylogTheme: Decorator = (Story) => (
  <MemoryRouter>
    <GraylogThemeProvider initialThemeModeOverride="light" userIsLoggedIn={false}>
      <GlobalStyles />
      <Story />
    </GraylogThemeProvider>
  </MemoryRouter>
);

const preview: Preview = {
  decorators: [withGraylogTheme],
};

export default preview;
