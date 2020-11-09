import React from 'react';

import AppRouter from 'routing/AppRouter';
import ThemeAndUserProvider from 'contexts/ThemeAndUserProvider';

import StreamsProvider from '../contexts/StreamsProvider';

const LoggedInPage = () => {
  return (
    <ThemeAndUserProvider>
      <StreamsProvider>
        <AppRouter />
      </StreamsProvider>
    </ThemeAndUserProvider>
  );
};

export default LoggedInPage;
