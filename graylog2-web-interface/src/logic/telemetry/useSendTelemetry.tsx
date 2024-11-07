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
import type { DataRouterContextObject } from 'react-router/dist/lib/context';

import type { TelemetryEventType, TelemetryEvent } from 'logic/telemetry/TelemetryContext';
import TelemetryContext from 'logic/telemetry/TelemetryContext';
import { currentPathname, stripPrefixFromPathname } from 'util/URLUtils';
import { singleton } from 'logic/singleton';

const retrieveCurrentRoute = (dataRouterContext: DataRouterContextObject) => {
  if (!dataRouterContext?.router?.routes) {
    return undefined;
  }

  const { router: { routes } } = dataRouterContext;
  const pathname = currentPathname();
  const matches = matchRoutes(routes, pathname);

  return stripPrefixFromPathname(matches?.at(-1)?.route.path);
};

const useSendTelemetry = () => {
  const { sendTelemetry } = useContext(TelemetryContext);
  const dataRouterContext = useContext(DataRouterContext);

  return useCallback((eventType: TelemetryEventType, event: Optional<TelemetryEvent, 'app_path_pattern'>) => {
    const route = retrieveCurrentRoute(dataRouterContext);

    return sendTelemetry(
      eventType,
      { app_path_pattern: route, ...event },
    );
  }, [dataRouterContext, sendTelemetry]);
};

export default singleton('core.useSendTelemetry', () => useSendTelemetry);
