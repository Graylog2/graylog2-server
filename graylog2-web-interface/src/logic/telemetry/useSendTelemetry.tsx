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
import { useCallback, useContext } from 'react';
import type { Optional } from 'utility-types';
import { UNSAFE_DataRouterContext as DataRouterContext, matchRoutes } from 'react-router-dom';

import type { TelemetryEventType, TelemetryEvent } from 'logic/telemetry/TelemetryContext';
import TelemetryContext from 'logic/telemetry/TelemetryContext';
import { currentPathnameWithoutPrefix } from 'util/URLUtils';

const useSendTelemetry = () => {
  const { sendTelemetry } = useContext(TelemetryContext);
  const dataRouterContext = useContext(DataRouterContext);

  return useCallback((eventType: TelemetryEventType, event: Optional<TelemetryEvent, 'app_path_pattern'>) => {
    if (!dataRouterContext?.router?.routes) {
      throw new Error('Data router context is missing!');
    }

    const { router: { routes } } = dataRouterContext;
    const pathname = currentPathnameWithoutPrefix();
    const matches = matchRoutes(routes, pathname);
    const route = matches.at(-1).route.path;

    return sendTelemetry(
      eventType,
      { app_path_pattern: route, ...event },
    );
  }, [dataRouterContext, sendTelemetry]);
};

export default useSendTelemetry;
