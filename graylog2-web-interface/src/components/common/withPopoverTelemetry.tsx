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
// eslint-disable-next-line no-restricted-imports
import type { PopoverProps } from 'react-bootstrap';
import React, { useEffect } from 'react';
import kebabCase from 'lodash/kebabCase';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const withPopoverTelemetry = (Component) => {
  return (props: PopoverProps) => {
    const { 'data-event-element': eventElement, 'data-app-section': appSection }: any = props;
    const sendTelemetry = useSendTelemetry();

    useEffect(() => {
      const telemetryEvent = {
        app_section: kebabCase(appSection),
        app_action_value: kebabCase(eventElement),
      };

      if (telemetryEvent.app_action_value) {
        sendTelemetry('popover_open', telemetryEvent);
      }

      return () => {
        if (telemetryEvent.app_action_value) {
          sendTelemetry('popover_close', telemetryEvent);
        }
      };
    }, [eventElement, appSection, sendTelemetry]);

    return <Component {...props} />;
  };
};

export default withPopoverTelemetry;
