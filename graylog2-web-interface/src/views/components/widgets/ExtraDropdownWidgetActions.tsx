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
import useViewsDispatch from 'views/stores/useViewsDispatch';
import useWidgetActions from 'views/components/widgets/useWidgetActions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import type { ActionComponents } from 'views/components/actions/ActionHandler';
import type { WidgetActionType, WidgetAction } from 'views/components/widgets/Types';
import generateId from 'logic/generateId';

type SetComponents = (value: ((prevState: ActionComponents) => ActionComponents) | ActionComponents) => void;
type Props = {
  widget: Widget;
  setComponents: SetComponents;
};

const createHandler = (action: WidgetActionType, setComponents: SetComponents): WidgetAction => {
  if (action.action) {
    return action.action;
  }
  if (action.component) {
    const ActionComponent = action.component;

    return (widget, contexts) => () => {
      const id = generateId();

      const onClose = () => setComponents(({ [id]: _, ...rest }) => rest);
      const renderedComponent = (
        <ActionComponent key={action.type} widget={widget} contexts={contexts} onClose={onClose} />
      );

      setComponents((actionComponents) => ({
        [id]: renderedComponent,
        ...actionComponents,
      }));

      return Promise.resolve();
    };
  }

  return () => () => Promise.resolve();
};

const ExtraDropdownWidgetActions = ({ widget, setComponents }: Props) => {
  const widgetFocusContext = useContext(WidgetFocusContext);
  const pluginWidgetActions = useWidgetActions();
  const dispatch = useViewsDispatch();
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  const extraWidgetActions = useMemo(
    () =>
      pluginWidgetActions
        .filter(
          ({ isHidden = () => false, position }) =>
            !isHidden(widget) && (position === 'dropdown' || position === undefined),
        )
        .map((action) => {
          const handler = createHandler(action, setComponents);
          const _onSelect = () => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.SEARCH_WIDGET_EXTRA_ACTION, {
              app_pathname: getPathnameWithoutId(pathname),
              app_section: 'search-widget',
              app_action_value: action.type,
            });

            dispatch(handler(widget, { widgetFocusContext }));
          };
          const disabled = action.disabled?.() ?? false;

          return (
            <MenuItem key={`${action.type}-${widget.id}`} disabled={disabled} onSelect={_onSelect}>
              {action.title(widget)}
            </MenuItem>
          );
        }),
    [dispatch, pathname, pluginWidgetActions, sendTelemetry, setComponents, widget, widgetFocusContext],
  );

  return extraWidgetActions.length > 0 ? (
    <>
      <MenuItem divider />
      {extraWidgetActions}
    </>
  ) : null;
};

export default ExtraDropdownWidgetActions;
