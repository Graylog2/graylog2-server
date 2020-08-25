// @flow strict
import * as React from 'react';
import { useMemo } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { breakpoints, colors, fonts, utils } from 'theme';
import type { ThemeInterface } from 'theme';
import usePrefersColorScheme from 'hooks/usePrefersColorScheme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';

import useCurrentThemeMode from './UseCurrentThemeMode';

type Props = {
  children: React.Node,
};

const createTheme = (mode, themeColors, changeMode): ThemeInterface => {
  const formattedUtils = {
    ...utils,
    colorLevel: utils.colorLevel(themeColors),
    readableColor: utils.readableColor(themeColors),
  };

  return {
    mode,
    changeMode,
    breakpoints,
    colors: themeColors,
    fonts,
    components: {
      button: buttonStyles({ colors: themeColors, utils: formattedUtils }),
      aceEditor: aceEditorStyles({ colors: themeColors }),
    },
    utils: formattedUtils,
  };
};

const GraylogThemeProvider = ({ children }: Props) => {
  const colorScheme = usePrefersColorScheme();
  const [mode, setCurrentThemeMode] = useCurrentThemeMode(colorScheme);

  const themeColors = colors[mode];

  const theme = useMemo(() => (themeColors ? createTheme(mode, themeColors, setCurrentThemeMode) : undefined), [themeColors]);

  return theme ? (
    <ThemeProvider theme={theme}>
      {children}
    </ThemeProvider>
  ) : null;
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
