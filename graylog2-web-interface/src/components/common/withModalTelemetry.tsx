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
import type { ModalProps } from 'react-bootstrap';
import React, { useEffect } from 'react';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const withModalTelemetry = (Component) => {
  return (props: ModalProps) => {
    const { show, 'data-event-element': eventElement, 'data-app-section': appSection }: any = props;
    const sendTelemetry = useSendTelemetry();

    useEffect(() => {
      const telemetryEvent = {
        appSection,
        eventElement,
        eventInfo: {},
      };

      if (show && eventElement) {
        sendTelemetry('open', telemetryEvent);
      }

      return () => {
        if (show && eventElement) {
          sendTelemetry('close', telemetryEvent);
        }
      };
    }, [show, eventElement, appSection, sendTelemetry]);

    return <Component {...props} />;
  };
};

export default withModalTelemetry;
