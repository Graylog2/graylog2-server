import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { color, util } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';

/* NOTE: mode will eventually need to come from User Preferences */
const THEME_MODE = 'teinte';

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeProvider theme={{
      mode: THEME_MODE,
      color: color[THEME_MODE],
      util,
      components: {
        button: buttonStyles({ color: color[THEME_MODE] }),
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
