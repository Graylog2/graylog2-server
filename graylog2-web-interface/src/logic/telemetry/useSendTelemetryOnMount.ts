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
import { useEffect } from 'react';
import type { Optional } from 'utility-types';

import type { TelemetryEventType, TelemetryEvent } from 'logic/telemetry/TelemetryContext';
import type useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const useSendTelemetryOnMount = (
  sendTelemetry: ReturnType<typeof useSendTelemetry>,
  eventType: TelemetryEventType,
  event: Optional<TelemetryEvent, 'app_path_pattern'>,
  deps: Array<unknown> = [],
) => {
  useEffect(() => {
    sendTelemetry(eventType, event);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sendTelemetry, ...deps]);
};

export default useSendTelemetryOnMount;
