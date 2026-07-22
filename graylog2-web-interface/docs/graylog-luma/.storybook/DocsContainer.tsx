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
import { DocsContainer as BaseDocsContainer } from '@storybook/addon-docs/blocks';
import type { DocsContainerProps } from '@storybook/addon-docs/blocks';
import { useDarkMode } from 'storybook-dark-mode';
import { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT } from '@graylog/sawmill';
import { createGlobalStyle, css } from 'styled-components';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import GlobalThemeStyles from 'theme/GlobalThemeStyles';

import { darkTheme, lightTheme } from './storybook-theme';

const DocsOverride = createGlobalStyle(
  ({ theme }) => css`
    p:not(.sb-anchor):not(.sb-unstyled),
    li:not(.sb-unstyled) {
      font-size: 1rem;
    }

    h2 {
      border-bottom: none;
    }

    .docs-story {
      background-color: ${theme.colors.global.background};
    }

    /* Storybook's DocsContent :where(a) rule resets margin to 0, overriding OcticonAnchor's margin-left: -24px in dark mode */
    h1 a[aria-hidden='true'],
    h2 a[aria-hidden='true'],
    h3 a[aria-hidden='true'],
    h4 a[aria-hidden='true'],
    h5 a[aria-hidden='true'],
    h6 a[aria-hidden='true'] {
      margin-left: -24px;
    }
  `,
);

export const DocsContainer = (props: DocsContainerProps) => {
  const isDark = useDarkMode();
  const colorScheme = isDark ? COLOR_SCHEME_DARK : COLOR_SCHEME_LIGHT;

  return (
    <GraylogThemeProvider initialThemeModeOverride={colorScheme} key={colorScheme} userIsLoggedIn>
      <GlobalThemeStyles />
      <DocsOverride />
      <BaseDocsContainer {...props} theme={isDark ? darkTheme : lightTheme} />
    </GraylogThemeProvider>
  );
};
