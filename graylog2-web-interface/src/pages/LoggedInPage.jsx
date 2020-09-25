import React from 'react';

import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';
import AppRouter from 'routing/AppRouter';
import CurrentUserProvider from 'contexts/CurrentUserProvider';

import StreamsProvider from '../contexts/StreamsProvider';

const LoggedInPage = () => {
  return (
    <CurrentUserProvider>
      <CurrentUserPreferencesProvider>
        <StreamsProvider>
          <AppRouter />
        </StreamsProvider>
      </CurrentUserPreferencesProvider>
    </CurrentUserProvider>
  );
};

export default LoggedInPage;
