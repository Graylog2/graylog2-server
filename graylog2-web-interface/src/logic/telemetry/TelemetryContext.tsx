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

import { singleton } from 'logic/singleton';
import type { EventType } from 'logic/telemetry/types';

export type TelemetryEvent = {
  app_path_pattern?: string,
  app_pathname?: string,
  app_section?: string,
  app_action_value?: string
  [key: string]: unknown,
};

export type TelemetryEventType = '$pageview' | EventType;

type ContextType = {
  sendTelemetry: (eventType: TelemetryEventType, event: TelemetryEvent | { [key: string]: unknown }) => void,
}
const TelemetryContext = React.createContext<ContextType>({
  sendTelemetry: () => {
  },
});

export default singleton('contexts.TelemetryContext', () => TelemetryContext);
