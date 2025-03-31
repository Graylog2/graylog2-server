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

// This file contains telemetry functions which are used in multiple components
import { useCallback } from 'react';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';

export const useSendWidgetEditTelemetry = () => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  return useCallback(
    () =>
      sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.WIDGET_EDIT_TOGGLED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'search-widget',
        app_action_value: 'widget-edit-button',
      }),
    [pathname, sendTelemetry],
  );
};

export const useSendWidgetEditCancelTelemetry = () => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  return useCallback(
    () =>
      sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.WIDGET_EDIT_CANCEL_CLICKED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'search-widget',
        app_action_value: 'widget-edit-cancel-button',
      }),
    [pathname, sendTelemetry],
  );
};

export const useSendWidgetConfigUpdateTelemetry = () => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  return useCallback(
    () =>
      sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.WIDGET_CONFIG_UPDATED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'search-widget',
        app_action_value: 'widget-edit-update-button',
      }),
    [pathname, sendTelemetry],
  );
};
