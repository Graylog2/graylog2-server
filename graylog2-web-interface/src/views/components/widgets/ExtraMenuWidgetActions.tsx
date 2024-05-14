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
import useWidgetActions from 'views/components/widgets/useWidgetActions';
import ExportWidgetPlugAction from 'views/components/widgets/ExportWidgetAction/ExportWidgetPlugAction';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';

type Props = {
  widget: Widget,
};

const ExtraMenuWidgetActions = ({ widget }: Props) => {
  const widgetFocusContext = useContext(WidgetFocusContext);
  const pluginWidgetActions = useWidgetActions();

  const extraWidgetActions = useMemo(() => {
    const filtratedActions = pluginWidgetActions
      .filter(({ isHidden = () => false, position }) => !isHidden(widget) && position === 'menu');

    const hasExportAction = filtratedActions.some(({ type }) => type === 'export-widget-action');

    if (!hasExportAction && widget.type === AggregationWidget.type) {
      filtratedActions.push(ExportWidgetPlugAction);
    }

    return filtratedActions.map(({ component: Component, type, disabled = () => false }) => (
      <Component widget={widget}
                 contexts={{ widgetFocusContext }}
                 key={`${type}-${widget.id}`}
                 disabled={disabled()} />
    ));
  },
  [pluginWidgetActions, widget, widgetFocusContext]);

  return extraWidgetActions.length > 0
    ? extraWidgetActions
    : null;
};

export default ExtraMenuWidgetActions;
