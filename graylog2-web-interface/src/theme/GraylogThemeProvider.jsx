import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { teinte } from 'theme';
import GlobalThemeStyles from 'theme/GlobalThemeStyles';

/* NOTE: mode can be `teinte` or `noire` and will eventually need to come from User Preferences */
const THEME_MODE = 'teinte';

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeProvider theme={{ mode: THEME_MODE, color: teinte }}>
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
