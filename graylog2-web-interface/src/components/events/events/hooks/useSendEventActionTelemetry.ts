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
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const useSendEventActionTelemetry = () => {
  const sendTelemetry = useSendTelemetry('alerts-and-events');

  return useCallback(
    (actionName: string, fromBulk: boolean, eventDetails: { [key: string]: unknown } = {}) =>
      sendTelemetry(TELEMETRY_EVENT_TYPE.ALERTS_AND_EVENTS.ACTION_RAN, {
        event_details: {
          actionName,
          fromBulk: !!fromBulk,
          ...eventDetails,
        },
      }),
    [sendTelemetry],
  );
};

export default useSendEventActionTelemetry;
