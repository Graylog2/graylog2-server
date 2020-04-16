import React from 'react';
import AppRouter from 'routing/AppRouter';
import CurrentUserPreferencesProvider from '../contexts/CurrentUserPreferencesProvider';

const LoggedInPage = () => (
  <CurrentUserPreferencesProvider>
    <AppRouter />
  </CurrentUserPreferencesProvider>
);

export default LoggedInPage;
