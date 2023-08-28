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
import { useTheme } from 'styled-components';
import type { MantineThemeOverride } from '@mantine/core';
import { Global, MantineProvider } from '@mantine/core';

type Props = {
  children: React.ReactElement,
};

const ThemeWrapper = ({ children }: Props) => {
  const theme = useTheme();

  const mantineTheme: MantineThemeOverride = {
    colorScheme: theme.mode === 'teint' ? 'light' : 'dark',
    fontFamily: theme.fonts.family.body,
    fontFamilyMonospace: theme.fonts.family.monospace,
    fontSizes: {
      xs: theme.fonts.size.tiny.value,
      sm: theme.fonts.size.small.value,
      md: theme.fonts.size.body.value,
      lg: theme.fonts.size.large.value,
      xl: theme.fonts.size.huge.value,
    },
    headings: {
      fontFamily: theme.fonts.family.body,
      sizes: {
        h1: {
          fontSize: theme.fonts.size.h1.rem,
        },
        h2: {
          fontSize: theme.fonts.size.h2.rem,
        },
        h3: {
          fontSize: theme.fonts.size.h3.rem,
        },
        h4: {
          fontSize: theme.fonts.size.h4.rem,
        },
        h5: {
          fontSize: theme.fonts.size.h5.rem,
        },
        h6: {
          fontSize: theme.fonts.size.h6.rem,
        },
      },
    },
    spacing: {
      xxs: theme.spacings.xxs,
      xs: theme.spacings.xs,
      sm: theme.spacings.sm,
      md: theme.spacings.md,
      lg: theme.spacings.lg,
      xl: theme.spacings.xl,
    },
    components: {
      Text: {
        defaultProps: {
          color: theme.colors.global.textDefault,
        },
      },
      Anchor: {
        styles: () => ({
          root: {
            color: theme.colors.global.link,
            '&:hover': {
              color: theme.colors.global.linkHover,
            },
          },
        }),
      },
    },
  };

  const globalStyles = () => ({
    body: {
      backgroundColor: theme.colors.global.background,
      color: theme.colors.global.textDefault,
    },
  });

  return (
    <MantineProvider theme={mantineTheme}>
      <Global styles={globalStyles} />
      {children}
    </MantineProvider>
  );
};

export default ThemeWrapper;
