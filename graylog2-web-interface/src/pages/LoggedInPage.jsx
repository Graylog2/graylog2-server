import React, { useContext, useEffect } from 'react';

import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';
import AppRouter from 'routing/AppRouter';
import CurrentUserProvider from 'contexts/CurrentUserProvider';
import { GlobalStylesContext } from 'contexts/GlobalStylesProvider';

const LoggedInPage = () => {
  const { addGlobalStyles } = useContext(GlobalStylesContext);

  useEffect(() => {
    addGlobalStyles(null);
  }, []);

  return (
    <CurrentUserProvider>
      <CurrentUserPreferencesProvider>
        <AppRouter />
      </CurrentUserPreferencesProvider>
    </CurrentUserProvider>
  );
};

export default LoggedInPage;
