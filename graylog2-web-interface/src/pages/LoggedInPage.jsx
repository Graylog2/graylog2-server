import React from 'react';
import AppRouter from 'routing/AppRouter';
import CurrentUserPreferencesProvider from '../contexts/CurrentUserPreferencesProvider';

import StreamsProvider from '../contexts/StreamsProvider';

const LoggedInPage = () => (
  <CurrentUserPreferencesProvider>
    <StreamsProvider>
      <AppRouter />
    </StreamsProvider>
  </CurrentUserPreferencesProvider>
);

export default LoggedInPage;
