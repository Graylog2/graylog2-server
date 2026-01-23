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
import { useContext, useCallback } from 'react';

import WidgetContext from 'views/components/contexts/WidgetContext';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { TelemetryEvent } from 'logic/telemetry/TelemetryContext';

type KeyType = keyof typeof TELEMETRY_EVENT_TYPE.FAVORITE_FIELDS;

const useSendFavoriteFieldTelemetry = () => {
  const widget = useContext(WidgetContext);
  const sendTelemetry = useSendTelemetry();
  
  const widgetType = widget?.type ?? 'permalink';

  return useCallback(
    (key: KeyType, extra: TelemetryEvent = {}) =>
      sendTelemetry(TELEMETRY_EVENT_TYPE.FAVORITE_FIELDS?.[key], {
        app_section: widgetType,
        ...extra,
      }),
    [sendTelemetry, widgetType],
  );
};

export default useSendFavoriteFieldTelemetry;
