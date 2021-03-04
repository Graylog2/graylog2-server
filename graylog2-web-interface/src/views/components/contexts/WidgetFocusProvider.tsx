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
import { useState, useEffect, useCallback } from 'react';
import { useLocation, useHistory } from 'react-router-dom';
import PropTypes from 'prop-types';
import URI from 'urijs';

import { useStore } from 'stores/connect';
import useQuery from 'routing/useQuery';
import { WidgetStore } from 'views/stores/WidgetStore';
import WidgetFocusContext, { FocusedWidget } from 'views/components/contexts/WidgetFocusContext';

const _syncWithQuery = (query: string, focusedWidget: FocusedWidget) => {
  const baseUri = new URI(query)
    .removeSearch('focused')
    .removeSearch('editing');

  if (focusedWidget?.id) {
    return baseUri
      .setSearch('focused', focusedWidget.id)
      .setSearch('editing', focusedWidget.editing)
      .toString();
  }

  return baseUri.toString();
};

const useFocusWidgetIdFromParam = (focusedWidget, setFocusedWidget, widgets) => {
  const { focused: paramFocusedWidget, editing: paramEditing } = useQuery();

  useEffect(() => {
    if (focusedWidget?.id !== paramFocusedWidget) {
      if (paramFocusedWidget && !widgets.has(paramFocusedWidget)) {
        return;
      }

      setFocusedWidget({ id: paramFocusedWidget, editing: paramEditing === 'true' });
    }
  }, [focusedWidget, paramFocusedWidget, setFocusedWidget, widgets, paramEditing]);
};

const WidgetFocusProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const { search, pathname } = useLocation();
  const query = pathname + search;
  const history = useHistory();
  const [focusedWidget, setFocusedWidget] = useState<FocusedWidget | undefined>();
  const widgets = useStore(WidgetStore);

  useFocusWidgetIdFromParam(focusedWidget, setFocusedWidget, widgets);

  useEffect(() => {
    if (focusedWidget && !widgets.has(focusedWidget.id)) {
      setFocusedWidget(undefined);
    }
  }, [focusedWidget, widgets]);

  const updateFocus = useCallback((widget: FocusedWidget | undefined) => {
    const newFocusWidget = widget?.id === focusedWidget?.id
      ? undefined
      : widget;
    const newURI = _syncWithQuery(query, newFocusWidget);

    history.replace(newURI);
  }, [focusedWidget, history, query]);

  return (
    <WidgetFocusContext.Provider value={{ focusedWidget, setFocusedWidget: updateFocus }}>
      {children}
    </WidgetFocusContext.Provider>
  );
};

WidgetFocusProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default WidgetFocusProvider;
