import React, { useContext } from 'react';
import { ThemeContext } from 'styled-components';
import type { MantineThemeOverride } from '@mantine/core';
import { Global, MantineProvider } from '@mantine/core';

type Props = {
  children: React.ReactElement,
};

const ThemeWrapper = ({ children }: Props) => {
  const theme = useContext(ThemeContext);

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
      xs: theme.spacings.px.xs,
      sm: theme.spacings.px.sm,
      lg: theme.spacings.px.lg,
      xl: theme.spacings.px.xl,
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
