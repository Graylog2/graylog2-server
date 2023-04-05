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

export type TelemetryEvent = {
  appSection?: string,
  eventElement?: string
  eventInfo?: {
    [key: string]: string | boolean | number,
  }
};

export type TelemetryEventType = '$pageview' | 'view' | 'click' | 'submit_form' | 'open_popover'
  | 'close_popover' | 'toggle_input_button' | 'change_input_value' | 'close_modal' | 'open_modal';

type ContextType = {
  sendTelemetry: (eventType: TelemetryEventType, event: TelemetryEvent) => void,
}
const TelemetryContext = React.createContext<ContextType>({
  sendTelemetry: () => {
  },
});

export default singleton('contexts.TelemetryContext', () => TelemetryContext);
