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

import type { TelemetryEventType, TelemetryEvent } from 'logic/telemetry/TelemetryContext';
import TelemetryContext from 'logic/telemetry/TelemetryContext';
import useRoutePattern from 'routing/useRoutePattern';

const useSendTelemetry = () => {
  const { sendTelemetry } = useContext(TelemetryContext);
  const route = useRoutePattern();

  return useCallback((eventType: TelemetryEventType, event: Optional<TelemetryEvent, 'app_path_pattern'>) => sendTelemetry(
    eventType,
    { app_path_pattern: route, ...event },
  ), [sendTelemetry, route]);
};

export default useSendTelemetry;
