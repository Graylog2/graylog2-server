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
import { useCallback } from 'react';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import type { TelemetryEvent, TelemetryEventType } from 'logic/telemetry/TelemetryContext';

export type CollectorsSection =
  | 'collectors-overview'
  | 'collectors-fleets'
  | 'collectors-fleet-detail'
  | 'collectors-instances'
  | 'collectors-deployment'
  | 'collectors-settings';

const sectionForPathname = (pathname: string): CollectorsSection => {
  if (pathname.includes('/system/collectors/fleets/')) return 'collectors-fleet-detail';
  if (pathname.endsWith('/system/collectors/fleets')) return 'collectors-fleets';
  if (pathname.includes('/system/collectors/instances')) return 'collectors-instances';
  if (pathname.includes('/system/collectors/deployment')) return 'collectors-deployment';
  if (pathname.includes('/system/collectors/settings')) return 'collectors-settings';

  return 'collectors-overview';
};

const useSendCollectorsTelemetry = () => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  return useCallback(
    (eventType: TelemetryEventType, event: TelemetryEvent) => {
      const section = sectionForPathname(pathname);

      sendTelemetry(eventType, { app_section: section, ...event });
    },
    [pathname, sendTelemetry],
  );
};

export default useSendCollectorsTelemetry;
