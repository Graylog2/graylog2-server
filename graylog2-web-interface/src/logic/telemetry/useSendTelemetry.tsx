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
import { useCallback, useContext, useMemo } from 'react';
import type { Optional } from 'utility-types';
import { UNSAFE_DataRouterContext as DataRouterContext, matchRoutes } from 'react-router-dom';

import type { TelemetryEventType, TelemetryEvent } from 'logic/telemetry/TelemetryContext';
import TelemetryContext from 'logic/telemetry/TelemetryContext';
import AppConfig from 'util/AppConfig';

const useSendTelemetry = () => {
  const { sendTelemetry } = useContext(TelemetryContext);
  const dataRouterContext = useContext(DataRouterContext);
  const pathPrefixLength = useMemo(() => {
    const pathPrefix = AppConfig.gl2AppPathPrefix();

    return (!pathPrefix || pathPrefix === '' || pathPrefix === '/') ? 0 : pathPrefix.length;
  }, []);

  return useCallback((eventType: TelemetryEventType, event: Optional<TelemetryEvent, 'app_path_pattern'>) => {
    const { router: { routes } } = dataRouterContext;
    const pathname = window.location.pathname.slice(pathPrefixLength);
    const matches = matchRoutes(routes, pathname);
    const route = matches.at(-1).route.path;

    return sendTelemetry(
      eventType,
      { app_path_pattern: route, ...event },
    );
  }, [sendTelemetry, dataRouterContext]);
};

export default useSendTelemetry;
