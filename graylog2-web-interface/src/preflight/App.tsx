/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { AppShell } from '@mantine/core';
import { useState } from 'react';

import Navigation from 'preflight/navigation/Navigation';
import Setup from 'preflight/components/Setup';
import WaitingForStartup from 'preflight/components/WaitingForStartup';
import ErrorBoundary from 'preflight/components/ErrorBoundary';

const App = () => {
  const [isWaitingForStartup, setIsWaitingForStartup] = useState(false);

  return (
    <AppShell padding="md" header={{ height: 80 }}>
      <Navigation />
      <AppShell.Main>
        <ErrorBoundary>
          {!isWaitingForStartup && <Setup setIsWaitingForStartup={setIsWaitingForStartup} />}
          {isWaitingForStartup && <WaitingForStartup />}
        </ErrorBoundary>
      </AppShell.Main>
    </AppShell>
  );
};

export default App;
