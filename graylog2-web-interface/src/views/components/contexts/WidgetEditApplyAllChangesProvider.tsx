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
import { useState, useRef, useCallback } from 'react';

import { WidgetActions } from 'views/stores/WidgetStore';
import Widget from 'views/logic/widgets/Widget';
import UserNotification from 'util/UserNotification';

import WidgetEditApplyAllChangesContext from './WidgetEditApplyAllChangesContext';

type Props = {
  widget: Widget,
  children: React.ReactNode,
}

const WidgetEditApplyAllChangesProvider = ({ children, widget }: Props) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const applySearchControlsChanges = useRef(null);
  const applyElementConfigurationChanges = useRef(null);

  const bindApplySearchControlsChanges = useCallback((updateWidgetConfig) => {
    applySearchControlsChanges.current = updateWidgetConfig;
  }, []);

  const bindApplyElementConfigurationChanges = useCallback((updateWidget) => {
    applyElementConfigurationChanges.current = updateWidget;
  }, []);

  const applyAllWidgetChanges = useCallback(() => {
    setIsSubmitting(true);
    let newWidget = widget;

    if (applySearchControlsChanges.current) {
      newWidget = applySearchControlsChanges.current(newWidget);
    }

    if (applyElementConfigurationChanges.current) {
      const newConfig = applyElementConfigurationChanges.current(newWidget.config);
      newWidget = newWidget.toBuilder().config(newConfig).build();
    }

    return WidgetActions.update(widget.id, newWidget)
      .catch((error) => {
        UserNotification.error(`Applying widget changes failed with status: ${error}`);

        return error;
      }).finally(() => setIsSubmitting(false));
  }, [widget]);

  const contextValue = {
    applyAllWidgetChanges,
    bindApplyElementConfigurationChanges,
    bindApplySearchControlsChanges,
    isSubmitting,
  };

  return (
    <WidgetEditApplyAllChangesContext.Provider value={contextValue}>
      {children}
    </WidgetEditApplyAllChangesContext.Provider>
  );
};

export default WidgetEditApplyAllChangesProvider;
