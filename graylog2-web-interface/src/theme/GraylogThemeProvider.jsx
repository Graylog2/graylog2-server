import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { color } from 'theme';
import GlobalThemeStyles from './GlobalThemeStyles';

/* NOTE: mode can be `teinte` or `noire` and will eventually need to come from User Preferences */
const THEME_MODE = 'teinte';

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeProvider theme={{ mode: THEME_MODE, color: color[THEME_MODE] }}>
      <>
        <GlobalThemeStyles />
        {children}
      </>
    </ThemeProvider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
