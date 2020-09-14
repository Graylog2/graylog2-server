import React, { useContext, useEffect } from 'react';

import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';
import AppRouter from 'routing/AppRouter';
import CurrentUserProvider from 'contexts/CurrentUserProvider';
import { GlobalStylesContext } from 'contexts/GlobalStylesProvider';

import StreamsProvider from '../contexts/StreamsProvider';

const LoggedInPage = () => {
  const { addGlobalStyles } = useContext(GlobalStylesContext);

  useEffect(() => {
    addGlobalStyles(null);
  }, []);

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
