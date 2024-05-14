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
import { useContext, useMemo } from 'react';

import type Widget from 'views/logic/widgets/Widget';
import { MenuItem } from 'components/bootstrap';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import useAppDispatch from 'stores/useAppDispatch';
import useWidgetActions from 'views/components/widgets/useWidgetActions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

type Props = {
  widget: Widget,
};

const ExtraDropdownWidgetActions = ({ widget }: Props) => {
  const widgetFocusContext = useContext(WidgetFocusContext);
  const pluginWidgetActions = useWidgetActions();
  const dispatch = useAppDispatch();
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const extraWidgetActions = useMemo(() => pluginWidgetActions
    .filter(({ isHidden = () => false, position }) => !isHidden(widget) && (position === 'dropdown' || position === undefined))
    .map(({ title, action, type, disabled = () => false }) => {
      const _onSelect = () => {
        sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.SEARCH_WIDGET_EXTRA_ACTION, {
          app_pathname: getPathnameWithoutId(pathname),
          app_section: 'search-widget',
          app_action_value: type,
        });

        dispatch(action(widget, { widgetFocusContext }));
      };

      return (
        <MenuItem key={`${type}-${widget.id}`} disabled={disabled()} onSelect={_onSelect}>{title(widget)}</MenuItem>);
    }), [dispatch, pathname, pluginWidgetActions, sendTelemetry, widget, widgetFocusContext]);

  return extraWidgetActions.length > 0
    ? (
      <>
        <MenuItem divider />
        {extraWidgetActions}
      </>
    )
    : null;
};

export default ExtraDropdownWidgetActions;
