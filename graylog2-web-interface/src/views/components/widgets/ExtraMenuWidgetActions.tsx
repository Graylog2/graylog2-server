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
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import useAppDispatch from 'stores/useAppDispatch';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import useMenuWidgetActions from 'views/components/widgets/useMenuWidgetActions';

type Props = {
  widget: Widget,
};

const ExtraMenuWidgetActions = ({ widget }: Props) => {
  const widgetFocusContext = useContext(WidgetFocusContext);
  const pluginWidgetActions = useMenuWidgetActions();

  const dispatch = useAppDispatch();
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const extraWidgetActions = useMemo(() => pluginWidgetActions
    .filter(({ isHidden = () => false }) => !isHidden(widget))
    .map(({ component: Component, type, disabled = () => false }) => (
      <Component widget={widget} contexts={{ widgetFocusContext }} key={`${type}-${widget.id}`} disabled={disabled()} />)), [dispatch, pathname, pluginWidgetActions, sendTelemetry, widget, widgetFocusContext]);
  console.log({ pluginWidgetActions, extraWidgetActions });

  return extraWidgetActions.length > 0
    ? (
      <>
        {extraWidgetActions}
      </>
    )
    : null;
};

export default ExtraMenuWidgetActions;
