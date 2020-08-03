import React from 'react';

import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';
import AppRouter from 'routing/AppRouter';
import CurrentUserProvider from 'contexts/CurrentUserProvider';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import GlobalThemeStyles from 'theme/GlobalThemeStyles';

const LoggedInPage = () => (
  <CurrentUserProvider>
    <CurrentUserPreferencesProvider>
      <GraylogThemeProvider>
        <GlobalThemeStyles />
        <AppRouter />
      </GraylogThemeProvider>
    </CurrentUserPreferencesProvider>
  </CurrentUserProvider>
);

export default LoggedInPage;
