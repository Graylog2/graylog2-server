// @flow strict
import * as React from 'react';
import { useMemo } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { breakpoints, colors, fonts, utils } from 'theme';
import type { ThemeInterface } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';

import useCurrentThemeMode from './UseCurrentThemeMode';

type Props = {
  children: React.Node,
  overrideMode: ?string,
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

const GraylogThemeProvider = ({ children, overrideMode }: Props) => {
  const [mode, setCurrentThemeMode] = useCurrentThemeMode(overrideMode);

  const themeColors = colors[mode];

  const generatedTheme = themeColors ? createTheme(mode, themeColors, setCurrentThemeMode) : undefined;
  const theme = useMemo(() => (generatedTheme), [mode, themeColors]);

  return theme ? (
    <ThemeProvider theme={theme}>
      {children}
    </ThemeProvider>
  ) : null;
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
  overrideMode: PropTypes.string,
};

GraylogThemeProvider.defaultProps = {
  overrideMode: undefined,
};

export default GraylogThemeProvider;
