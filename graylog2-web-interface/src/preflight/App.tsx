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
import { useState, useCallback } from 'react';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import Navigation from 'preflight/navigation/Navigation';
import Setup from 'preflight/components/Setup';
import useDataNodes from 'preflight/hooks/useDataNodes';
import UserNotification from 'preflight/util/UserNotification';
import WaitingForStartup from 'preflight/components/WaitingForStartup';

const App = () => {
  const { data: dataNodes } = useDataNodes();
  const [waitingForStartup, setWaitingForStartup] = useState(false);

  const onResumeStartup = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (dataNodes?.length || window.confirm('Are you sure you want to resume startup without a running Graylog data node?')) {
      fetch('POST', qualifyUrl('/api/status/finish-config'), undefined, false)
        .then(() => {
          setWaitingForStartup(true);
        })
        .catch((error) => {
          setWaitingForStartup(false);

          UserNotification.error(`Resuming startup failed with error: ${error}`,
            'Could not resume startup');
        });
    }
  }, [dataNodes?.length]);

  return (
    <AppShell padding="md" header={<Navigation />}>
      {!waitingForStartup && <Setup onResumeStartup={onResumeStartup} />}
      {waitingForStartup && <WaitingForStartup />}
    </AppShell>
  );
};

export default App;
