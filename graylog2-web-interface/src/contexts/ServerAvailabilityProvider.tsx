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
import { useState, useCallback, useEffect, useMemo, useRef } from 'react';

import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { registerCallbacks, reportError, reportSuccess } from 'api/server-availability';
import type { ServerError } from 'contexts/ServerAvailabilityContext';

import ServerAvailabilityContext from './ServerAvailabilityContext';

type ServerState = { up: true } | { up: false; error: ServerError };

const ServerAvailabilityProvider = ({ children }: React.PropsWithChildren) => {
  const [server, setServer] = useState<ServerState>({ up: true });
  const [version, setVersion] = useState<string | undefined>(undefined);
  const versionRef = useRef(version);

  useEffect(() => {
    versionRef.current = version;
  }, [version]);

  const ping = useCallback(
    () =>
      window
        .fetch(qualifyUrl(ApiRoutes.ping().url), {
          method: 'GET',
          headers: {
            Accept: 'application/json',
            'X-Graylog-No-Session-Extension': 'true',
          },
        })
        .then((resp) => resp.json())
        .then((response: { version?: string }) => {
          if (response?.version !== versionRef.current) {
            setVersion(response?.version);
          }

          reportSuccess();
        })
        .catch((error) => {
          reportError(error);
        }),
    [],
  );

  useEffect(() => {
    ping();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    registerCallbacks(
      (error) => setServer((prev) => (prev.up ? { up: false, error: error as ServerError } : prev)),
      () => setServer((prev) => (!prev.up ? { up: true } : prev)),
    );

    return () => registerCallbacks(null, null);
  }, []);

  const value = useMemo(() => ({ server, version, ping }), [server, version, ping]);

  return <ServerAvailabilityContext.Provider value={value}>{children}</ServerAvailabilityContext.Provider>;
};

export default ServerAvailabilityProvider;
