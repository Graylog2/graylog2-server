import React from 'react';
import AppRouter from 'routing/AppRouter';
import CurrentUserProvider from 'contexts/CurrentUserProvider';
import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';

const LoggedInPage = () => (
  <CurrentUserProvider>
    <CurrentUserPreferencesProvider>
      <AppRouter />
    </CurrentUserPreferencesProvider>
  </CurrentUserProvider>
);

export default LoggedInPage;
