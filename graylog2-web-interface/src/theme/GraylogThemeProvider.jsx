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
  defaultMode: ?string,
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

const GraylogThemeProvider = ({ children, defaultMode }: Props) => {
  const [mode, setCurrentThemeMode] = useCurrentThemeMode();
  const themeMode = defaultMode ?? mode;

  const themeColors = colors[themeMode];

  const theme = useMemo(() => (themeColors ? createTheme(themeMode, themeColors, setCurrentThemeMode) : undefined), [themeMode, themeColors]);

  return theme ? (
    <ThemeProvider theme={theme}>
      {children}
    </ThemeProvider>
  ) : null;
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
  defaultMode: PropTypes.string,
};

GraylogThemeProvider.defaultProps = {
  defaultMode: undefined,
};

export default GraylogThemeProvider;
