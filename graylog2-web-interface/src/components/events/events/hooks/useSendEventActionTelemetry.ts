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
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

const useSendEventActionTelemetry = () => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const { activePerspective } = useActivePerspective();

  return useCallback((actionName: string, fromBulk: boolean) => sendTelemetry(TELEMETRY_EVENT_TYPE.ALERTS_AND_EVENTS.ACTION_RAN, {
    app_pathname: getPathnameWithoutId(pathname),
    app_section: 'alerts-and-events',
    event_details: {
      actionName,
      fromBulk: !!fromBulk,
      perspectiveId: activePerspective.id,
    },
  }), [activePerspective.id, pathname, sendTelemetry]);
};

export default useSendEventActionTelemetry;
