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

import Widget from 'views/logic/widgets/Widget';
import usePluginEntities from 'views/logic/usePluginEntities';
import { MenuItem } from 'components/graylog';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

type Props = {
  onSelect: (eventKey: string, e: MouseEvent) => void,
  widget: Widget,
};

const ExtraWidgetActions = ({ onSelect, widget }: Props) => {
  const widgetFocusContext = useContext(WidgetFocusContext);
  const pluginWidgetActions = usePluginEntities('views.widgets.actions');
  const extraWidgetActions = useMemo(() => pluginWidgetActions
    .filter(({ isHidden = () => false }) => !isHidden(widget))
    .map(({ title, action, type, disabled }) => {
      const _onSelect = (eventKey: string, e: MouseEvent) => {
        action(widget, { widgetFocusContext });
        onSelect(eventKey, e);
      };

      const isDisabled = disabled && disabled();

      return (<MenuItem key={`${type}-${widget.id}`} disabled={isDisabled} onSelect={_onSelect}>{title(widget)}</MenuItem>);
    }), [onSelect, pluginWidgetActions, widget, widgetFocusContext]);

  return extraWidgetActions.length > 0
    ? (
      <>
        <MenuItem divider />
        {extraWidgetActions}
      </>
    )
    : null;
};

export default ExtraWidgetActions;
