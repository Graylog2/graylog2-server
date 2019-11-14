import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import GlobalThemeStyles from './GlobalThemeStyles';

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeProvider theme={{ mode: 'teinte' }}>
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
