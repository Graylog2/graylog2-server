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
import { useContext, useEffect, useRef } from 'react';

import View from 'views/logic/views/View';
import useView from 'views/hooks/useView';
import useIsNew from 'views/hooks/useIsNew';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import TelemetryContext from 'logic/telemetry/TelemetryContext';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const IlluminateDashboardViewTelemetry = () => {
  const view = useView();
  const isNew = useIsNew();
  const sendTelemetry = useSendTelemetry();
  const telemetryContext = useContext(TelemetryContext);
  const lastHandled = useRef<{ viewId: string; telemetryContext: typeof telemetryContext }>(undefined);
  const isIlluminateDashboard = view.type === View.Type.Dashboard && view.scope === 'ILLUMINATE';
  const viewId = view.id;
  const viewTitle = view.title;

  useEffect(() => {
    // The previous view stays in the store while the next one loads, so a path change alone must not send.
    // A new telemetry context still sends, since events are dropped until telemetry is ready after a fresh page load.
    if (lastHandled.current?.viewId === viewId && lastHandled.current?.telemetryContext === telemetryContext) {
      return;
    }

    lastHandled.current = { viewId, telemetryContext };

    if (isIlluminateDashboard && !isNew) {
      sendTelemetry(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.ILLUMINATE_DASHBOARD_VIEWED, {
        app_action_value: 'illuminate-dashboard-view',
        event_details: { dashboard_title: viewTitle },
      });
    }
  }, [isIlluminateDashboard, isNew, sendTelemetry, telemetryContext, viewId, viewTitle]);

  return null;
};

export default IlluminateDashboardViewTelemetry;
