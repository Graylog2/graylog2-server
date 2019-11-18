import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { color } from 'theme';
import GlobalThemeStyles from './GlobalThemeStyles';

const DEFAULT_THEME = 'teinte';

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeProvider theme={{ mode: DEFAULT_THEME, color: color[DEFAULT_THEME] }}>
      {/* NOTE: mode can be `teinte` and will eventually need to come from User Preferences */}
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
