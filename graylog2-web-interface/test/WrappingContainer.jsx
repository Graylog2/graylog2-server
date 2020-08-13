import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { breakpoints, colors, fonts, utils } from 'theme';
import { THEME_MODE_LIGHT } from 'theme/constants';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';

const WrappingContainer = ({ children }) => {
  const themeColors = colors[THEME_MODE_LIGHT];
  const formattedUtils = {
    ...utils,
    colorLevel: utils.colorLevel(themeColors),
    readableColor: utils.readableColor(themeColors),
  };

  const theme = {
    mode: THEME_MODE_LIGHT,
    changeMode: () => {},
    breakpoints,
    colors: themeColors,
    fonts,
    components: {
      button: buttonStyles({ colors: themeColors, utils: formattedUtils }),
      aceEditor: aceEditorStyles({ colors: themeColors }),
    },
    utils: formattedUtils,
  };

  return (
    <ThemeProvider theme={theme}>
      {children}
    </ThemeProvider>
  );
};

WrappingContainer.propTypes = {
  children: PropTypes.node.isRequired,
};

export default WrappingContainer;
