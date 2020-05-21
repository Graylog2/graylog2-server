import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { breakpoints, colors, utils } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';

/* NOTE: mode will eventually need to come from User Preferences */
const THEME_MODE = 'teinte';

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeProvider theme={{
      mode: THEME_MODE,
      colors,
      breakpoints,
      utils,
      components: {
        button: buttonStyles({ colors }),
      },
    }}>
      {children}
    </ThemeProvider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
