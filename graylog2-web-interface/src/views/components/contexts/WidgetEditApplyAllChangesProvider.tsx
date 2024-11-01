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
import { useContext, useRef, useCallback, useMemo } from 'react';

import type Widget from 'views/logic/widgets/Widget';
import UserNotification from 'util/UserNotification';
import DisableSubmissionStateContext from 'views/components/contexts/DisableSubmissionStateContext';
import useAppDispatch from 'stores/useAppDispatch';
import { updateWidget } from 'views/logic/slices/widgetActions';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';

import WidgetEditApplyAllChangesContext from './WidgetEditApplyAllChangesContext';

const useBindApplyChanges = () => {
  const applyChangesRef = useRef(null);

  const bindApplyChanges = useCallback((updateWidgetConfig) => {
    applyChangesRef.current = updateWidgetConfig;
  }, []);

  return { applyChangesRef, bindApplyChanges };
};

type Props = {
  widget: Widget,
  children: React.ReactNode,
}

const useApplyAllWidgetChanges = (
  widget: Widget,
  applySearchControlsChanges: React.RefObject<(widget: Widget) => Widget>,
  applyElementConfigurationChanges: React.RefObject<(widgetConfig: WidgetConfig) => WidgetConfig>,
) => {
  const { setDisabled } = useContext(DisableSubmissionStateContext);
  const dispatch = useAppDispatch();
  const setDisableWidgetEditSubmit = useCallback(
    (disabled: boolean) => setDisabled('widget-edit-apply-all-changes', disabled),
    [setDisabled]);

  return useCallback(() => {
    let newWidget = widget;
    let hasChanges = false;

    if (applySearchControlsChanges.current) {
      const updatedWidget = applySearchControlsChanges.current(newWidget);

      if (updatedWidget) {
        newWidget = updatedWidget;
        hasChanges = hasChanges || true;
      }
    }

    if (applyElementConfigurationChanges.current) {
      const updatedWidgetConfig = applyElementConfigurationChanges.current(newWidget.config);

      if (updatedWidgetConfig) {
        newWidget = newWidget.toBuilder().config(updatedWidgetConfig).build();
        hasChanges = hasChanges || true;
      }
    }

    if (hasChanges) {
      setDisableWidgetEditSubmit(true);

      return dispatch(updateWidget(widget.id, newWidget))
        .catch((error) => {
          UserNotification.error(`Applying widget changes failed with status: ${error}`);

          return error;
        }).finally(() => setDisableWidgetEditSubmit(false));
    }

    return Promise.resolve();
  }, [widget, applySearchControlsChanges, applyElementConfigurationChanges, setDisableWidgetEditSubmit, dispatch]);
};

const WidgetEditApplyAllChangesProvider = ({ children, widget }: Props) => {
  const { applyChangesRef: applySearchControlsChanges, bindApplyChanges: bindApplySearchControlsChanges } = useBindApplyChanges();
  const { applyChangesRef: applyElementConfigurationChanges, bindApplyChanges: bindApplyElementConfigurationChanges } = useBindApplyChanges();
  const applyAllWidgetChanges = useApplyAllWidgetChanges(widget, applySearchControlsChanges, applyElementConfigurationChanges);

  const contextValue = useMemo(() => ({
    applyAllWidgetChanges,
    bindApplyElementConfigurationChanges,
    bindApplySearchControlsChanges,
  }), [
    applyAllWidgetChanges,
    bindApplyElementConfigurationChanges,
    bindApplySearchControlsChanges,
  ]);

  return (
    <WidgetEditApplyAllChangesContext.Provider value={contextValue}>
      {children}
    </WidgetEditApplyAllChangesContext.Provider>
  );
};

export default WidgetEditApplyAllChangesProvider;
