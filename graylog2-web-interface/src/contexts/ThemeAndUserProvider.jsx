import React from 'react';
import PropTypes from 'prop-types';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import GlobalThemeStyles from 'theme/GlobalThemeStyles';

import CurrentUserPreferencesProvider from './CurrentUserPreferencesProvider';
import CurrentUserProvider from './CurrentUserProvider';

const ThemeAndUserProvider = ({ children }) => {
  return (
    <CurrentUserProvider>
      <CurrentUserPreferencesProvider>
        <GraylogThemeProvider>
          <GlobalThemeStyles />
          {children}
        </GraylogThemeProvider>
      </CurrentUserPreferencesProvider>
    </CurrentUserProvider>
  );
};

ThemeAndUserProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default ThemeAndUserProvider;
