import React from 'react';

import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';
import AppRouter from 'routing/AppRouter';
import CurrentUserProvider from 'contexts/CurrentUserProvider';

const LoggedInPage = () => (
  <CurrentUserProvider>
    <CurrentUserPreferencesProvider>
      <AppRouter />
    </CurrentUserPreferencesProvider>
  </CurrentUserProvider>
);

export default LoggedInPage;
