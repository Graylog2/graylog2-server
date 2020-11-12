// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { breakpoints, colors, fonts, utils } from 'theme';
import type { ThemeInterface } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';

import useCurrentThemeMode from './UseCurrentThemeMode';

type Props = {
  children: React.Node,
};

const GraylogThemeProvider = ({ children }: Props) => {
  const [mode, changeMode] = useCurrentThemeMode();
  const themeColors = colors[mode];

  const theme = useCallback((): ThemeInterface => {
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
  }, [mode, themeColors, changeMode]);

  return (
    <ThemeProvider theme={theme}>
      {children}
    </ThemeProvider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
