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

import { WidgetActions } from 'views/stores/WidgetStore';
import type Widget from 'views/logic/widgets/Widget';
import UserNotification from 'util/UserNotification';
import DisableSubmissionStateContext from 'views/components/contexts/DisableSubmissionStateContext';

import WidgetEditApplyAllChangesContext from './WidgetEditApplyAllChangesContext';

type Props = {
  widget: Widget,
  children: React.ReactNode,
}

const WidgetEditApplyAllChangesProvider = ({ children, widget }: Props) => {
  const { setDisabled } = useContext(DisableSubmissionStateContext);
  const setDisableWidgetEditSubmit = useCallback((disabled: boolean) => setDisabled('widget-edit-apply-all-changes', disabled), [setDisabled]);
  const applySearchControlsChanges = useRef(null);
  const applyElementConfigurationChanges = useRef(null);

  const bindApplySearchControlsChanges = useCallback((updateWidgetConfig) => {
    applySearchControlsChanges.current = updateWidgetConfig;
  }, []);

  const bindApplyElementConfigurationChanges = useCallback((updateWidget) => {
    applyElementConfigurationChanges.current = updateWidget;
  }, []);

  const applyAllWidgetChanges = useCallback(() => {
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

      return WidgetActions.update(widget.id, newWidget)
        .catch((error) => {
          UserNotification.error(`Applying widget changes failed with status: ${error}`);

          return error;
        }).finally(() => setDisableWidgetEditSubmit(false));
    }

    return Promise.resolve();
  }, [widget, setDisableWidgetEditSubmit]);

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
