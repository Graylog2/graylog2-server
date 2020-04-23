import React from 'react';

import AppRouter from 'routing/AppRouter';

import CurrentUserPreferencesProvider from '../contexts/CurrentUserPreferencesProvider';
import CustomizationProvider from '../contexts/CustomizationProvider';

const LoggedInPage = () => (
  <CustomizationProvider>
    <CurrentUserPreferencesProvider>
      <AppRouter />
    </CurrentUserPreferencesProvider>
  </CustomizationProvider>
);

export default LoggedInPage;
