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

type Props = {
  onSelect: (eventKey: string, e: MouseEvent) => void,
  widget: Widget,
};

const ExtraWidgetActions = ({ onSelect, widget }: Props) => {
  const widgetFocusContext = useContext(WidgetFocusContext);
  const pluginWidgetActions = useWidgetActions();
  const dispatch = useAppDispatch();
  const extraWidgetActions = useMemo(() => pluginWidgetActions
    .filter(({ isHidden = () => false }) => !isHidden(widget))
    .map(({ title, action, type, disabled = () => false }) => {
      const _onSelect = (eventKey: string, e: MouseEvent) => {
        dispatch(action(widget, { widgetFocusContext }));
        onSelect(eventKey, e);
      };

      return (<MenuItem key={`${type}-${widget.id}`} disabled={disabled()} onSelect={_onSelect}>{title(widget)}</MenuItem>);
    }), [dispatch, onSelect, pluginWidgetActions, widget, widgetFocusContext]);

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
